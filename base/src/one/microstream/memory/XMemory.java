package one.microstream.memory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Vector;

import one.microstream.exceptions.InstantiationRuntimeException;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;


/**
 * Util class for low-level VM memory operations and information that makes the call site independent of
 * a certain JVM implementation (e.g. java.misc.Unsafe).
 *
 * @author Thomas Muenz
 */
public final class XMemory
{
	///////////////////////////////////////////////////////////////////////////
	// constants //
	//////////////

	private static final Unsafe VM = (Unsafe)getSystemInstance();

	// better calculate it once instead of making wild assumptions that can change (e.g. 64 bit coops has only 12 byte)
	private static final int BYTE_SIZE_OBJECT_HEADER = calculateByteSizeObjectHeader();

	// According to tests and investigation, memory alignment is always 8 bytes, even for 32 bit JVMs.
	private static final int
		MEMORY_ALIGNMENT_FACTOR =                           8,
		MEMORY_ALIGNMENT_MODULO = MEMORY_ALIGNMENT_FACTOR - 1,
		MEMORY_ALIGNMENT_MASK   = ~MEMORY_ALIGNMENT_MODULO
	;

	// constant names documenting that a value shall be shifted by n bits. Also to get CheckStyle off my back.
	private static final int
		BITS1 = 1,
		BITS2 = 2,
		BITS3 = 3
	;

	// CHECKSTYLE.OFF: ConstantName: type names are intentionally unchanged
	private static final long
		OFFSET_ArrayList_elementData     = internalGetFieldOffset(ArrayList.class    , "elementData"      ),
		OFFSET_ArrayList_size            = internalGetFieldOffset(ArrayList.class    , "size"             ),
		OFFSET_HashSet_map               = internalGetFieldOffset(HashSet.class      , "map"              ),
		OFFSET_HashMap_loadFactor        = internalGetFieldOffset(HashMap.class      , "loadFactor"       ),
		OFFSET_Hashtable_loadFactor      = internalGetFieldOffset(Hashtable.class    , "loadFactor"       ),
		OFFSET_LinkedHashMap_loadFactor  = internalGetFieldOffset(LinkedHashMap.class, "loadFactor"       ),
		OFFSET_LinkedHashMap_accessOrder = internalGetFieldOffset(LinkedHashMap.class, "accessOrder"      ),
		OFFSET_PriorityQueue_queue       = internalGetFieldOffset(PriorityQueue.class, "queue"            ),
		OFFSET_PriorityQueue_size        = internalGetFieldOffset(PriorityQueue.class, "size"             ),
		OFFSET_Vector_elementData        = internalGetFieldOffset(Vector.class       , "elementData"      ),
		OFFSET_Vector_elementCount       = internalGetFieldOffset(Vector.class       , "elementCount"     ),
		OFFSET_Vector_capacityIncrement  = internalGetFieldOffset(Vector.class       , "capacityIncrement"),
		OFFSET_Properties_Defaults       = internalGetFieldOffset(Properties.class   , "defaults"         )
	;
	// CHECKSTYLE.ON: ConstantName
	
	private static DirectByteBufferDeallocator DIRECT_BYTEBUFFER_DEALLOCATOR = createDefaultDirectByteBufferDeallocator();
	
	/**
	 * Allows to set the {@link DirectByteBufferDeallocator} used by
	 * {@link #deallocateDirectByteBuffer(ByteBuffer)}.<br>
	 * See {@link DirectByteBufferDeallocator} for details.
	 * <p>
	 * The passed instance "should" be immutable or better stateless to ensure concurrency-safe usage,
	 * but ultimately, the responsibility resides with the author of the instance's implementation.
	 * <p>
	 * Passing <code>null</code> resets to the internal default implementation.
	 * The used deallocator will never be null.
	 * 
	 * @param deallocator the deallocator to be used.
	 * 
	 * @see DirectByteBufferDeallocator
	 */
	public static synchronized void setDirectByteBufferDeallocator(
		final DirectByteBufferDeallocator deallocator
	)
	{
		// allows resetting to default without knowing what the default is.
		DIRECT_BYTEBUFFER_DEALLOCATOR = deallocator != null
			? deallocator
			: createDefaultDirectByteBufferDeallocator()
		;
	}
	
	public static synchronized DirectByteBufferDeallocator getDirectByteBufferDeallocator()
	{
		return DIRECT_BYTEBUFFER_DEALLOCATOR;
	}
	
	public static DirectByteBufferDeallocator createDefaultDirectByteBufferDeallocator()
	{
		return DirectByteBufferDeallocator.NoOp();
	}
	
	
	// return type not specified to avoid public API dependencies to sun implementation details
	public static final Object getSystemInstance()
	{
		// all that clumsy detour ... x_x
		if(XMemory.class.getClassLoader() == null)
		{
			return Unsafe.getUnsafe(); // Not on bootclasspath
		}
		try
		{
			final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			return theUnsafe.get(XMemory.class);
		}
		catch(final Exception e)
		{
			throw new Error("Could not obtain access to sun.misc.Unsafe", e);
		}
	}
	
	public static long objectFieldOffset(final Field field)
	{
		return VM.objectFieldOffset(field);
	}

	public static long[] objectFieldOffsets(final Field[] fields)
	{
		final long[] offsets = new long[fields.length];
		for(int i = 0; i < fields.length; i++)
		{
			if(Modifier.isStatic(fields[i].getModifiers()))
			{
				throw new IllegalArgumentException("Not an instance field: " + fields[i]);
			}
			offsets[i] = (int)VM.objectFieldOffset(fields[i]);
		}
		return offsets;
	}

	private static long internalGetFieldOffset(final Class<?> type, final String declaredFieldName)
	{
		// minimal algorithm, only for local use
		for(Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
		{
			try
			{
				for(final Field field : c.getDeclaredFields())
				{
					if(field.getName().equals(declaredFieldName))
					{
						return VM.objectFieldOffset(field);
					}
				}
			}
			catch(final Exception e)
			{
				throw new Error(e); // explode and die :)
			}
		}
		throw new Error("Field not found: " + type.getName() + '#' + declaredFieldName);
	}

	public static final Object getStaticReference(final Field field)
	{
		if(!Modifier.isStatic(field.getModifiers()))
		{
			throw new IllegalArgumentException();
		}
		return VM.getObject(VM.staticFieldBase(field), VM.staticFieldOffset(field));
	}
	
	/**
	 * No idea if this method is really (still?) necesssary, but it sounds reasonable.
	 * See
	 * http://stackoverflow.com/questions/8462200/examples-of-forcing-freeing-of-native-memory-direct-bytebuffer-has-allocated-us
	 *
	 * @param directByteBuffer
	 */
	public static final void deallocateDirectByteBuffer(final ByteBuffer directByteBuffer)
	{
		DIRECT_BYTEBUFFER_DEALLOCATOR.deallocateDirectByteBuffer(directByteBuffer);
	}

	/**
	 * Just to encapsulate that clumsy cast.
	 *
	 * @param directByteBuffer
	 * @return
	 */
	public static final long getDirectByteBufferAddress(final ByteBuffer directByteBuffer)
	{
		return ((DirectBuffer)directByteBuffer).address();
	}
	
	/**
	 * Just to have all jdk internal types here at one place.
	 * 
	 * @param directByteBuffer
	 * @return
	 */
	public static final boolean isDirectByteBuffer(final ByteBuffer directByteBuffer)
	{
		return directByteBuffer instanceof DirectBuffer;
	}

	public static final ByteBuffer ensureDirectByteBufferCapacity(final ByteBuffer current, final long capacity)
	{
		if(current.capacity() >= capacity)
		{
			return current;
		}
		
		checkArrayRange(capacity);
		deallocateDirectByteBuffer(current);
		
		return ByteBuffer.allocateDirect((int)capacity);
	}

	public static Object[] accessArray(final ArrayList<?> arrayList)
	{
		// must check not null here explictely to prevent VM crashes
		return (Object[])VM.getObject(notNull(arrayList), OFFSET_ArrayList_elementData);
	}

	public static void setSize(final ArrayList<?> arrayList, final int size)
	{
		// must check not null here explictely to prevent VM crashes
		VM.putInt(notNull(arrayList), OFFSET_ArrayList_size, size);
	}

	/**
	 * My god. How incompetent can one be: they provide a constructor for configuring the load factor,
	 * but they provide no means to querying it. So if a hashset instance shall be transformed to another
	 * context and back (e.g. persistence), what is one supposed to do? Ignore the load factor and change
	 * the program behavior? What harm would it do to add an implementation-specific getter?
	 * <p>
	 * Not to mention the set wraps a map internally which is THE most moronic thing to do both memory-
	 * and performance-wise.
	 * <p>
	 * So another hack method has to provide basic functionality that is missing in the JDK.
	 * And should they ever get the idea to implement the set properly, this method will break.
	 *
	 * @param hashSet
	 * @return
	 */
	public static float getLoadFactor(final HashSet<?> hashSet)
	{
		// must check not null here explictely to prevent VM crashes
		final HashMap<?, ?> map = (HashMap<?, ?>)VM.getObject(notNull(hashSet), OFFSET_HashSet_map);
		return getLoadFactor(map);
	}

	public static float getLoadFactor(final HashMap<?, ?> hashMap)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getFloat(notNull(hashMap), OFFSET_HashMap_loadFactor);
	}

	public static float getLoadFactor(final Hashtable<?, ?> hashtable)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getFloat(notNull(hashtable), OFFSET_Hashtable_loadFactor);
	}

	public static float getLoadFactor(final LinkedHashMap<?, ?> linkedHashMap)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getFloat(notNull(linkedHashMap), OFFSET_LinkedHashMap_loadFactor);
	}

	public static boolean getAccessOrder(final LinkedHashMap<?, ?> linkedHashMap)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getBoolean(notNull(linkedHashMap), OFFSET_LinkedHashMap_accessOrder);
	}
	
	public static Object[] accessArray(final Vector<?> vector)
	{
		// must check not null here explictely to prevent VM crashes
		return (Object[])VM.getObject(notNull(vector), OFFSET_Vector_elementData);
	}
	
	public static int getElementCount(final Vector<?> vector)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getInt(notNull(vector), OFFSET_Vector_elementCount);
	}

	public static void setElementCount(final Vector<?> vector, final int size)
	{
		// must check not null here explictely to prevent VM crashes
		VM.putInt(notNull(vector), OFFSET_Vector_elementCount, size);
	}
	
	public static int getCapacityIncrement(final Vector<?> vector)
	{
		// must check not null here explictely to prevent VM crashes
		return VM.getInt(notNull(vector), OFFSET_Vector_capacityIncrement);
	}

	public static void setCapacityIncrement(final Vector<?> vector, final int size)
	{
		// must check not null here explictely to prevent VM crashes
		VM.putInt(notNull(vector), OFFSET_Vector_capacityIncrement, size);
	}
	
	public static Properties accessDefaults(final Properties properties)
	{
		// must check not null here explictely to prevent VM crashes
		return (Properties)VM.getObject(notNull(properties), OFFSET_Properties_Defaults);
	}

	public static void setDefaults(final Properties properties, final Properties defaults)
	{
		// must check not null here explictely to prevent VM crashes
		VM.putObject(notNull(properties), OFFSET_Properties_Defaults, defaults);
	}

	public static Object[] accessArray(final PriorityQueue<?> priorityQueue)
	{
		// must check not null here explictely to prevent VM crashes
		return (Object[])VM.getObject(notNull(priorityQueue), OFFSET_PriorityQueue_queue);
	}

	public static void setSize(final PriorityQueue<?> priorityQueue, final int size)
	{
		// must check not null here explictely to prevent VM crashes
		VM.putInt(notNull(priorityQueue), OFFSET_PriorityQueue_size, size);
	}
	

	
	public static final int bitSize_byte()
	{
		return Byte.SIZE;
	}

	public static final int bitSize_boolean()
	{
		return Byte.SIZE;
	}

	public static final int bitSize_short()
	{
		return Short.SIZE;
	}

	public static final int bitSize_char()
	{
		return Character.SIZE;
	}

	public static final int bitSize_int()
	{
		return Integer.SIZE;
	}

	public static final int bitSize_float()
	{
		return Float.SIZE;
	}

	public static final int bitSize_long()
	{
		return Long.SIZE;
	}

	public static final int bitSize_double()
	{
		return Double.SIZE;
	}



	///////////////////////////////////////////////////////////////////////////
	// memory byte size methods //
	/////////////////////////////

	public static final int byteSizeFieldValue(final Class<?> type)
	{
		return type.isPrimitive()
			? byteSizePrimitive(type)
			: byteSizeReference()
		;
	}

	public static final int byteSizePrimitive(final Class<?> type)
	{
		// onec again missing JDK functionality. Roughly ordered by probability.
		if(type == int.class)
		{
			return byteSize_int();
		}
		if(type == long.class)
		{
			return byteSize_long();
		}
		if(type == double.class)
		{
			return byteSize_double();
		}
		if(type == char.class)
		{
			return byteSize_char();
		}
		if(type == boolean.class)
		{
			return byteSize_boolean();
		}
		if(type == byte.class)
		{
			return byteSize_byte();
		}
		if(type == float.class)
		{
			return byteSize_float();
		}
		if(type == short.class)
		{
			return byteSize_short();
		}
				
		throw new IllegalArgumentException(); // intentionally covers void.class
	}

	public static final int byteSize_byte()
	{
		return Byte.BYTES;
	}

	public static final int byteSize_boolean()
	{
		return Byte.BYTES; // because JDK Pros can't figure out the length of a boolean value, obviously.
	}

	public static final int byteSize_short()
	{
		return Short.BYTES;
	}

	public static final int byteSize_char()
	{
		return Character.BYTES;
	}

	public static final int byteSize_int()
	{
		return Integer.BYTES;
	}

	public static final int byteSize_float()
	{
		return Float.BYTES;
	}

	public static final int byteSize_long()
	{
		return Long.BYTES;
	}

	public static final int byteSize_double()
	{
		return Double.BYTES;
	}

	public static final int byteSizeObjectHeader()
	{
		return BYTE_SIZE_OBJECT_HEADER;
	}

	public static final int byteSizeReference()
	{
		return Unsafe.ARRAY_OBJECT_INDEX_SCALE;
	}

	public static final long byteSizeArray_byte(final long elementCount)
	{
		return Unsafe.ARRAY_BYTE_BASE_OFFSET + Unsafe.ARRAY_BYTE_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_boolean(final long elementCount)
	{
		return Unsafe.ARRAY_BOOLEAN_BASE_OFFSET + Unsafe.ARRAY_BOOLEAN_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_short(final long elementCount)
	{
		return Unsafe.ARRAY_SHORT_BASE_OFFSET + Unsafe.ARRAY_SHORT_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_char(final long elementCount)
	{
		return Unsafe.ARRAY_CHAR_BASE_OFFSET + Unsafe.ARRAY_CHAR_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_int(final long elementCount)
	{
		return Unsafe.ARRAY_INT_BASE_OFFSET + Unsafe.ARRAY_INT_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_float(final long elementCount)
	{
		return Unsafe.ARRAY_FLOAT_BASE_OFFSET + Unsafe.ARRAY_FLOAT_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_long(final long elementCount)
	{
		return Unsafe.ARRAY_LONG_BASE_OFFSET + Unsafe.ARRAY_LONG_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArray_double(final long elementCount)
	{
		return Unsafe.ARRAY_DOUBLE_BASE_OFFSET + Unsafe.ARRAY_DOUBLE_INDEX_SCALE * elementCount;
	}

	public static final long byteSizeArrayObject(final long elementCount)
	{
		return Unsafe.ARRAY_OBJECT_BASE_OFFSET + Unsafe.ARRAY_OBJECT_INDEX_SCALE * elementCount;
	}

	private static final int calculateByteSizeObjectHeader()
	{
		// min logic should be unnecessary but better exclude any source for potential errors
		long minOffset = Long.MAX_VALUE;
		final Field[] declaredFields = XMemory.class.getDeclaredFields();
		for(final Field field : declaredFields)
		{
			if(Modifier.isStatic(field.getModifiers()))
			{
				continue;
			}
			if(VM.objectFieldOffset(field) < minOffset)
			{
				minOffset = VM.objectFieldOffset(field);
			}
		}
		if(minOffset == Long.MAX_VALUE)
		{
			throw new Error("Could not find object header dummy field in class " + XMemory.class);
		}
		return (int)minOffset; // offset of first instance field is guaranteed to be in int range ^^.
	}

	public static final int byteSizeInstance(final Class<?> type)
	{
		if(type.isPrimitive())
		{
			throw new IllegalArgumentException();
		}
		if(type.isArray())
		{
			// instance byte size accounts only array header (object header plus length field plus overhead)
			return VM.arrayBaseOffset(type);
		}
		if(type == Object.class)
		{
			// required because Object's super class is null (see below)
			return byteSizeObjectHeader();
		}

		// declared fields suffice as all super class fields are positioned before them
		final Field[] declaredFields = type.getDeclaredFields();
		long maxInstanceFieldOffset = 0;
		Field maxInstanceField = null;
		for(int i = 0; i < declaredFields.length; i++)
		{
			if(Modifier.isStatic(declaredFields[i].getModifiers()))
			{
				continue;
			}
			final long fieldOffset = VM.objectFieldOffset(declaredFields[i]);
//			XDebug.debugln(fieldOffset + "\t" + declaredFields[i]);
			if(fieldOffset >= maxInstanceFieldOffset)
			{
				maxInstanceField = declaredFields[i];
				maxInstanceFieldOffset = fieldOffset;
			}
		}

		// no declared instance field at all, fall back to super class fields recursively
		if(maxInstanceField == null)
		{
			return byteSizeInstance(type.getSuperclass());
		}

		// memory alignment is a wild assumption at this point. Hopefully it will always be true. Otherwise it's a bug.
		return (int)alignAddress(maxInstanceFieldOffset + byteSizeFieldValue(maxInstanceField.getType()));
	}

	public static final long alignAddress(final long address)
	{
		if((address & MEMORY_ALIGNMENT_MODULO) == 0)
		{
			return address; // already aligned
		}
		// According to tests and investigation, memory alignment is always 8 bytes, even for 32 bit JVMs.
		return (address & MEMORY_ALIGNMENT_MASK) + MEMORY_ALIGNMENT_FACTOR;
	}

	public static Field[] collectPrimitiveFieldsByByteSize(final Field[] fields, final int byteSize)
	{
		if(byteSize != byteSize_byte()
		&& byteSize != byteSize_short()
		&& byteSize != byteSize_int()
		&& byteSize != byteSize_long()
		)
		{
			throw new IllegalArgumentException("Invalid Java primitive byte size: " + byteSize);
		}

		final Field[] primFields = new Field[fields.length];
		int primFieldsCount = 0;
		for(int i = 0; i < fields.length; i++)
		{
			if(fields[i].getType().isPrimitive() && XMemory.byteSizePrimitive(fields[i].getType()) == byteSize)
			{
				primFields[primFieldsCount++] = fields[i];
			}
		}
		return Arrays.copyOf(primFields, primFieldsCount);
	}

	public static int calculatePrimitivesLength(final Field[] primFields)
	{
		int length = 0;
		for(int i = 0; i < primFields.length; i++)
		{
			if(!primFields[i].getType().isPrimitive())
			{
				throw new IllegalArgumentException("Not a primitive field: " + primFields[i]);
			}
			length += XMemory.byteSizePrimitive(primFields[i].getType());
		}
		return length;
	}

	public static Object getStaticFieldBase(final Field field)
	{
		return VM.staticFieldBase(notNull(field)); // throws IllegalArgumentException, so no need to check here
	}

	public static long[] staticFieldOffsets(final Field[] fields)
	{
		final long[] offsets = new long[fields.length];
		for(int i = 0; i < fields.length; i++)
		{
			if(!Modifier.isStatic(fields[i].getModifiers()))
			{
				throw new IllegalArgumentException("Not a static field: " + fields[i]);
			}
			offsets[i] = (int)VM.staticFieldOffset(fields[i]);
		}
		return offsets;
	}

	public static byte[] toByteArray(final long[] longArray)
	{
		final byte[] bytes = new byte[checkArrayRange((long)longArray.length << BITS3)];
		VM.copyMemory(longArray, Unsafe.ARRAY_LONG_BASE_OFFSET, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, bytes.length);
		return bytes;
	}

	public static byte[] toByteArray(final long value)
	{
		final byte[] bytes = new byte[byteSize_long()];
		put_long(bytes, 0, value);
		return bytes;
	}
	
	/**
	 * Arbitrary value that coincidently matches most hardware's page sizes
	 * without being hard-tied to Unsafe#pageSize.
	 * So this value is an educated guess and most of the time (almost always)
	 * a "good" value when paged-sized-ish buffer sizes are needed, while still
	 * not being at the mercy of an OS's JVM implementation.
	 * 
	 * @return a "good" value for a paged-sized-ish default buffer size.
	 */
	public static int defaultBufferSize()
	{
		return 4096;
	}
	
	/**
	 * Returns the system's memory "page size" (whatever that may be exactely for a given system).
	 * Use with care (and the dependency to a system value in mind!).
	 * 
	 * @return the system's memory "page size".
	 */
	public static int pageSize()
	{
		return VM.pageSize();
	}

	public static void put_short(final byte[] bytes, final int index, final short value)
	{
		VM.putShort(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void put_char(final byte[] bytes, final int index, final char value)
	{
		VM.putChar(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void put_int(final byte[] bytes, final int index, final int value)
	{
		VM.putInt(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void put_float(final byte[] bytes, final int index, final float value)
	{
		VM.putFloat(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void put_long(final byte[] bytes, final int index, final long value)
	{
		VM.putLong(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void put_double(final byte[] bytes, final int index, final double value)
	{
		VM.putDouble(bytes, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + index, value);
	}

	public static void _longInByteArray(final byte[] bytes, final long value)
	{
		VM.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, value);
	}

	public static long _longFromByteArray(final byte[] bytes)
	{
		return VM.getLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET);
	}



	public static final byte get_byte(final long address)
	{
		return VM.getByte(address);
	}

	public static final boolean get_boolean(final long address)
	{
		return VM.getBoolean(null, address);
	}

	public static final short get_short(final long address)
	{
		return VM.getShort(address);
	}

	public static final char get_char(final long address)
	{
		return VM.getChar(address);
	}

	public static final int get_int(final long address)
	{
		return VM.getInt(address);
	}

	public static final float get_float(final long address)
	{
		return VM.getFloat(address);
	}

	public static final long get_long(final long address)
	{
		return VM.getLong(address);
	}

	public static final double get_double(final long address)
	{
		return VM.getDouble(address);
	}

	public static final Object getObject(final long address)
	{
		return VM.getObject(null, address);
	}


	public static final byte get_byte(final Object instance, final long address)
	{
		return VM.getByte(instance, address);
	}

	public static final boolean get_boolean(final Object instance, final long address)
	{
		return VM.getBoolean(instance, address);
	}

	public static final short get_short(final Object instance, final long address)
	{
		return VM.getShort(instance, address);
	}

	public static final char get_char(final Object instance, final long address)
	{
		return VM.getChar(instance, address);
	}

	public static final int get_int(final Object instance, final long address)
	{
		return VM.getInt(instance, address);
	}

	public static final float get_float(final Object instance, final long address)
	{
		return VM.getInt(instance, address);
	}

	public static final long get_long(final Object instance, final long address)
	{
		return VM.getLong(instance, address);
	}

	public static final double get_double(final Object instance, final long address)
	{
		return VM.getInt(instance, address);
	}

	public static final Object getObject(final Object instance, final long address)
	{
		return VM.getObject(instance, address);
	}




	public static final void set_byte(final long address, final byte value)
	{
		VM.putByte(address, value);
	}

	public static final void set_boolean(final long address, final boolean value)
	{
		// where the heck is Unsafe#putBoolean(long, boolean)? Forgot to implement? Wtf?
		VM.putBoolean(null, address, value);
	}

	public static final void set_short(final long address, final short value)
	{
		VM.putShort(address, value);
	}

	public static final void set_char(final long address, final char value)
	{
		VM.putChar(address, value);
	}

	public static final void set_int(final long address, final int value)
	{
		VM.putInt(address, value);
	}

	public static final void set_float(final long address, final float value)
	{
		VM.putFloat(address, value);
	}

	public static final void set_long(final long address, final long value)
	{
		VM.putLong(address, value);
	}

	public static final void set_double(final long address, final double value)
	{
		VM.putDouble(address, value);
	}

	public static final void set_byte(final Object instance, final long offset, final byte value)
	{
		VM.putByte(instance, offset, value);
	}

	public static final void set_boolean(final Object instance, final long offset, final boolean value)
	{
		VM.putBoolean(instance, offset, value);
	}

	public static final void set_short(final Object instance, final long offset, final short value)
	{
		VM.putShort(instance, offset, value);
	}

	public static final void set_char(final Object instance, final long offset, final char value)
	{
		VM.putChar(instance, offset, value);
	}

	public static final void set_int(final Object instance, final long offset, final int value)
	{
		VM.putInt(instance, offset, value);
	}

	public static final void set_float(final Object instance, final long offset, final float value)
	{
		VM.putFloat(instance, offset, value);
	}

	public static final void set_long(final Object instance, final long offset, final long value)
	{
		VM.putLong(instance, offset, value);
	}

	public static final void set_double(final Object instance, final long offset, final double value)
	{
		VM.putDouble(instance, offset, value);
	}

	public static final void setObject(final Object instance, final long offset, final Object value)
	{
		VM.putObject(instance, offset, value);
	}

	public static final void copyRange(final long sourceAddress, final long targetAddress, final long length)
	{
		VM.copyMemory(sourceAddress, targetAddress, length);
	}

	public static final void copyRange(
		final Object source,
		final long   sourceOffset,
		final Object target,
		final long   targetOffset,
		final long   length
	)
	{
		VM.copyMemory(source, sourceOffset, target, targetOffset, length);
	}

	public static final void copyRangeToArray(final long sourceAddress, final byte[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_BYTE_BASE_OFFSET, target.length);
	}
	
	public static final void copyRangeToArray(
		final long   sourceAddress,
		final byte[] target       ,
		final int    targetIndex  ,
		final long   length
	)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_BYTE_BASE_OFFSET + targetIndex, length);
	}

	public static final void copyRangeToArray(final long sourceAddress, final boolean[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_BOOLEAN_BASE_OFFSET, target.length);
	}

	public static final void copyRangeToArray(final long sourceAddress, final short[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_SHORT_BASE_OFFSET, target.length << BITS1);
	}

	public static final void copyRangeToArray(final long sourceAddress, final char[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_CHAR_BASE_OFFSET, target.length << BITS1);
	}
	
	public static final void copyRangeToArray(
		final long   sourceAddress,
		final char[] target       ,
		final int    targetIndex  ,
		final long   targetLength
	)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_CHAR_BASE_OFFSET + (targetIndex << BITS1), targetLength << BITS1);
	}

	public static final void copyRangeToArray(final long sourceAddress, final int[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_INT_BASE_OFFSET, target.length << BITS2);
	}

	public static final void copyRangeToArray(final long sourceAddress, final float[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_FLOAT_BASE_OFFSET, target.length << BITS2);
	}

	public static final void copyRangeToArray(final long sourceAddress, final long[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_LONG_BASE_OFFSET, target.length << BITS3);
	}

	public static final void copyRangeToArray(final long sourceAddress, final double[] target)
	{
		VM.copyMemory(null, sourceAddress, target, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, target.length << BITS3);
	}

	
	
	// copyArrayToAddress //

	public static final void copyArrayToAddress(final byte[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, targetAddress, array.length);
	}

	public static final void copyArrayToAddress(
		final byte[] array        ,
		final int    offset       ,
		final int    length       ,
		final long   targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, targetAddress, length);
	}
	
	public static final void copyArrayToAddress(final boolean[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_BOOLEAN_BASE_OFFSET, null, targetAddress, array.length);
	}

	public static final void copyArrayToAddress(
		final boolean[] array        ,
		final int       offset       ,
		final int       length       ,
		final long      targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_BOOLEAN_BASE_OFFSET + offset, null, targetAddress, length);
	}
	
	public static final void copyArrayToAddress(final short[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_SHORT_BASE_OFFSET, null, targetAddress, array.length << BITS1);
	}

	public static final void copyArrayToAddress(
		final short[] array        ,
		final int     offset       ,
		final int     length       ,
		final long    targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_SHORT_BASE_OFFSET + (offset << BITS1), null, targetAddress, length << BITS1);
	}

	public static final void copyArrayToAddress(final char[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_CHAR_BASE_OFFSET, null, targetAddress, array.length << BITS1);
	}
	
	public static final void copyArrayToAddress(
		final char[] array        ,
		final int    offset       ,
		final int    length       ,
		final long   targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_CHAR_BASE_OFFSET + (offset << BITS1), null, targetAddress, length << BITS1);
	}
	
	public static final void copyArrayToAddress(final int[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_INT_BASE_OFFSET, null, targetAddress, array.length << BITS2);
	}

	public static final void copyArrayToAddress(
		final int[] array        ,
		final int   offset       ,
		final int   length       ,
		final long  targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_INT_BASE_OFFSET + (offset << BITS2), null, targetAddress, length << BITS2);
	}
	
	public static final void copyArrayToAddress(final float[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_FLOAT_BASE_OFFSET, null, targetAddress, array.length << BITS2);
	}

	public static final void copyArrayToAddress(
		final float[] array        ,
		final int     offset       ,
		final int     length       ,
		final long    targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_FLOAT_BASE_OFFSET + (offset << BITS2), null, targetAddress, length << BITS2);
	}
	
	public static final void copyArrayToAddress(final long[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_LONG_BASE_OFFSET, null, targetAddress, array.length << BITS3);
	}

	public static final void copyArrayToAddress(
		final long[]   array        ,
		final int      offset       ,
		final int      length       ,
		final long     targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_LONG_BASE_OFFSET + (offset << BITS3), null, targetAddress, length << BITS3);
	}
	
	public static final void copyArrayToAddress(final double[] array, final long targetAddress)
	{
		VM.copyMemory(array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, null, targetAddress, array.length << BITS3);
	}

	public static final void copyArrayToAddress(
		final double[] array        ,
		final int      offset       ,
		final int      length       ,
		final long     targetAddress
	)
	{
		VM.copyMemory(array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + (offset << BITS3), null, targetAddress, length << BITS3);
	}

	

	public static final byte get_byteFromBytes(final byte[] data, final int offset)
	{
		return VM.getByte(data, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + offset);
	}

	public static final boolean get_booleanFromBytes(final byte[] data, final int offset)
	{
		return VM.getBoolean(data, (long)Unsafe.ARRAY_BOOLEAN_BASE_OFFSET + offset);
	}

	public static final short get_shortFromBytes(final byte[] data, final int offset)
	{
		return VM.getShort(data, (long)Unsafe.ARRAY_SHORT_BASE_OFFSET + offset);
	}

	public static final char get_charFromBytes(final byte[] data, final int offset)
	{
		return VM.getChar(data, (long)Unsafe.ARRAY_CHAR_BASE_OFFSET + offset);
	}

	public static final int get_intFromBytes(final byte[] data, final int offset)
	{
		return VM.getInt(data, (long)Unsafe.ARRAY_INT_BASE_OFFSET + offset);
	}

	public static final float get_floatFromBytes(final byte[] data, final int offset)
	{
		return VM.getFloat(data, (long)Unsafe.ARRAY_FLOAT_BASE_OFFSET + offset);
	}

	public static final long get_longFromBytes(final byte[] data, final int offset)
	{
		return VM.getLong(data, (long)Unsafe.ARRAY_LONG_BASE_OFFSET + offset);
	}

	public static final double get_doubleFromBytes(final byte[] data, final int offset)
	{
		return VM.getDouble(data, (long)Unsafe.ARRAY_DOUBLE_BASE_OFFSET + offset);
	}

	public static final long allocate(final long bytes)
	{
		return VM.allocateMemory(bytes);
	}

	public static final long reallocate(final long address, final long bytes)
	{
		return VM.reallocateMemory(address, bytes);
	}

	public static final void fillRange(final long address, final long length, final byte value)
	{
		VM.setMemory(address, length, value);
	}

	public static final void free(final long address)
	{
		VM.freeMemory(address);
	}

	public static final boolean compareAndSwap_int(
		final Object subject    ,
		final long   offset     ,
		final int    expected   ,
		final int    replacement
	)
	{
		return VM.compareAndSwapInt(subject, offset, expected, replacement);
	}

	public static final boolean compareAndSwap_long(
		final Object subject    ,
		final long   offset     ,
		final long   expected   ,
		final long   replacement
	)
	{
		return VM.compareAndSwapLong(subject, offset, expected, replacement);
	}

	public static final boolean compareAndSwapObject(
		final Object subject    ,
		final long   offset     ,
		final Object expected   ,
		final Object replacement
	)
	{
		return VM.compareAndSwapObject(subject, offset, expected, replacement);
	}
	
	public static ByteOrder nativeByteOrder()
	{
		return ByteOrder.nativeOrder();
	}
	
	// because they (he) couldn't have implemented that where it belongs.
	public static ByteOrder resolveByteOrder(final String name)
	{
		if(name.equals(ByteOrder.BIG_ENDIAN.toString()))
		{
			return ByteOrder.BIG_ENDIAN;
		}
		if(name.equals(ByteOrder.LITTLE_ENDIAN.toString()))
		{
			return ByteOrder.LITTLE_ENDIAN;
		}
		
		// (31.10.2018 TM)EXCP: proper exception
		throw new RuntimeException("Unknown ByteOrder: \"" + name + "\"");
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T instantiate(final Class<T> c) throws InstantiationRuntimeException
	{
		try
		{
			return (T)VM.allocateInstance(c);
		}
		catch(final InstantiationException e)
		{
			throw new InstantiationRuntimeException(e);
		}
	}

	public static final byte[] directByteBufferToArray(final ByteBuffer directByteBuffer)
	{
		final byte[] bytes;
		copyRangeToArray(
			getDirectByteBufferAddress(directByteBuffer),
			bytes = new byte[directByteBuffer.limit()]
		);
		return bytes;
	}

	

	
	////////////////////////////////////////////////////////////////////////
	// some nasty util methods not directly related to memory operations //
	//////////////////////////////////////////////////////////////////////

	public static final void throwUnchecked(final Throwable t)
	{
		VM.throwException(t);
	}
	
	public static final void ensureClassInitialized(final Class<?>... classes)
	{
		for(final Class<?> c : classes)
		{
			ensureClassInitialized(c);
		}
	}

	public static final void ensureClassInitialized(final Class<?> c)
	{
		VM.ensureClassInitialized(c);
	}
	

	
	////////////////////////////////////////////////////////
	// copies of general logic to eliminate dependencies //
	//////////////////////////////////////////////////////
	
	private static final int checkArrayRange(final long capacity)
	{
		// " >= " proved to be faster in tests than ">" (probably due to simple sign checking)
		if(capacity > Integer.MAX_VALUE)
		{
			throw new IllegalArgumentException("Invalid array length: " + capacity);
		}
		
		return (int)capacity;
	}
	
	private static final <T> T notNull(final T object) throws NullPointerException
	{
		if(object == null)
		{
			// removing this method's stack trace entry is kind of a hack. On the other hand, it's not.
			throw new NullPointerException();
		}
		
		return object;
	}



	/* (18.09.2018 TM)TODO: fieldOffsetWorkaroundDummy necessary?
	 * Why is there no comment? If it is necessary, it has to be commented, why.
	 */
	Object fieldOffsetWorkaroundDummy;

	private XMemory()
	{
		throw new Error();
	}

}
