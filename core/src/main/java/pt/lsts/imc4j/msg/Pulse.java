package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;

/**
 * Hardware pulse detection.
 */
public class Pulse extends Message {
	public static final int ID_STATIC = 277;

	/**
	 * The time at which the pulse was dispatched from the original source.
	 * Represented in Universal Coordinated Time (UCT) in seconds since Jan 1, 1970 using IEEE
	 * double precision floating point numbers.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			units = "s"
	)
	public double origin_timestamp = 0;

	public String abbrev() {
		return "Pulse";
	}

	public int mgid() {
		return 277;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeDouble(origin_timestamp);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			origin_timestamp = buf.getDouble();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
