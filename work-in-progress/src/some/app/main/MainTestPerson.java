package some.app.main;

import one.microstream.entity.Entity;
import one.microstream.entity.EntityTransaction;
import some.app.entities.Person;
import some.app.entities._generated._Person.PersonCreator;
import some.app.entitylogging.EntityLogger;

/**
 * See {@link Entity} for an explanation of the concept.
 * 
 * @author TM
 *
 */
public class MainTestPerson
{
	// static only for simplicity of the example
	static final EntityTransaction             transaction = EntityTransaction.New();
//	static final EntityVersionContext<Integer> versions    = EntityVersionContext.New();
	
	
	public static void main(final String[] args)
	{
		final Person alice = PersonCreator.New()
			.firstName("Alice")
			.lastName("Allison")

			// add versioning
//			.addLayer(versions)
			
			// add logging
			.addLayer(EntityLogger.provideLogging())
						
			// add a generic transactional layer for concurrency handling
//			.$addLayer(transaction)
			
			/*
			 * Add ...
			 * access control layer,
			 * lazy loading timeout management layer,
			 * anything imaginable ...
			 * 
			 * The holy grail of entity handling.
			 * 
			 *      ~  _________  ~
			 *      ~  \       /  ~
			 *       ~  \  H  /  ~
			 *        ~  |   |  ~
			 *        ~  |___|  ~
			 */
			
			.create()
		;
				
		testSimpleGetterCall(alice);
		testCopy(alice);
//		testVersioning(alice);
	}
	
	static void testSimpleGetterCall(final Person p)
	{
		System.out.println("---[Simple Getter Call]---");
		System.out.println(p.firstName());
	}
	
	static void testCopy(final Person p)
	{
		System.out.println("\n\n---[Entity Copy]----------");
		
		final Person copy = PersonCreator
			.New(p)
			.lastName(p.lastName() + "-Modfified")
			.create()
		;
		System.out.println("orig.: " + p.lastName());
		System.out.println("copy : " + copy.lastName());
	}
	
//	static void testVersioning(final Person p)
//	{
//		System.out.println("\n\n---[Entity Versioning]-------");
//
//		final Person aliceData = Entity.data(p);
//
//		versions.currentVersion(1);
//		PersonUpdater.setLastName(p, aliceData.lastName() + "-v1");
//		versions.currentVersion(2);
//		PersonUpdater.setLastName(p, aliceData.lastName() + "-v2");
//		System.out.println("Version " + versions.currentVersion() + ": " + p.lastName());
//
//		versions.currentVersion(1);
//		System.out.println("Version " + versions.currentVersion() + ": " + p.lastName());
//
//		versions.currentVersion(null);
//		System.out.println("Version " + versions.currentVersion() + ": " + p.lastName());
//
//		versions.currentVersion(2);
//		System.out.println("Version " + versions.currentVersion() + ": " + p.lastName());
//	}
	
}
