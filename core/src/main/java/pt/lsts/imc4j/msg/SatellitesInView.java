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
 * This message is used to report the number of satellites in view per constellation. The constellation should be provided by the Entity label.
 */
public class SatellitesInView extends Message {
	public static final int ID_STATIC = 2023;

	/**
	 * Number of satellites in view.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int nsat = 0;

	public String abbrev() {
		return "SatellitesInView";
	}

	public int mgid() {
		return 2023;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte(nsat);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			nsat = buf.get() & 0xFF;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
