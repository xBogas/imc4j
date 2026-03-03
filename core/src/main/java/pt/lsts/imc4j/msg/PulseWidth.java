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
 * Logs the commanded pulse width for motors or servos.
 * Use the entity label to identify the channel.
 */
public class PulseWidth extends Message {
	public static final int ID_STATIC = 2025;

	/**
	 * The commanded pulse width in microseconds.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "µs"
	)
	public int value = 0;

	public String abbrev() {
		return "PulseWidth";
	}

	public int mgid() {
		return 2025;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(value);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			value = buf.getShort() & 0xFFFF;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
