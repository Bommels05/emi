package dev.emi.emi.backport;

import java.io.IOException;
import java.io.InputStream;

public class EmiResource {

	public interface ByteSource {
		InputStream open() throws IOException;
	}
	
	private final ByteSource src;
	
	public EmiResource(ByteSource src) {
		this.src = src;
	}

	public InputStream getInputStream() throws IOException {
		return src.open();
	}

}
