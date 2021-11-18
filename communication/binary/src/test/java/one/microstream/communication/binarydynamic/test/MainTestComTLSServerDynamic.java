package one.microstream.communication.binarydynamic.test;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;

import one.microstream.communication.binarydynamic.ComBinaryDynamic;
import one.microstream.communication.tls.ComTLSConnectionHandler;
import one.microstream.communication.tls.SecureRandomProvider;
import one.microstream.communication.tls.TLSKeyManagerProvider;
import one.microstream.communication.tls.TLSParametersProvider;
import one.microstream.communication.tls.TLSTrustManagerProvider;
import one.microstream.communication.types.ComHost;
import one.microstream.meta.XDebug;

public class MainTestComTLSServerDynamic
{
	public static void main(final String[] args)
	{
		Path serverKeyStore = Paths.get(args[0]);
		Path serverTrustStore = Paths.get(args[1]);
		
		final String largeString = createLargeString(10_000);
		XDebug.println("starting host...");
		
		final ComHost<?> host = ComBinaryDynamic.Foundation()
			.setHostByteOrder(ByteOrder.BIG_ENDIAN)
			.setConnectionHandler(ComTLSConnectionHandler.New(
					new TLSKeyManagerProvider.PKCS12(
						serverKeyStore,
						args[2].toCharArray()),
					new TLSTrustManagerProvider.PKCS12(
						serverTrustStore,
						args[2].toCharArray()),
					new TLSParametersProvider.Default(),
					new SecureRandomProvider.Default()
				))
			.setHostChannelAcceptor(hostChannel ->
			{
				hostChannel.send(new ComplexClassNew());
				
				hostChannel.send(largeString);
				
				final Object o = hostChannel.receive();
				XDebug.println("HOST RECEIVED: " + o.toString());
			})
			.createHost()
		;

		// run the host, making it constantly listen for new connections and relaying them to the logic
		host.run();
	}
	
	private static String createLargeString(final int lines)
	{
		String largeString = new String();
		
		for(int i = 1; i <= lines; i++)
		{
			largeString += i + " a large String with " + lines + " lines. This is line no. " + i + " of " + lines + "\n";
		}
		
		return largeString;
	}
}
