package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * Contains information as extracted from a digital S-57 chart.
 * This can be: location of static objects (buoys, beacons, etc), location and depth of depth contours,
 * location and depth of any other location contained in the chart.
 * For reference see Supervisors/Grounding.
 */
public class ENCAwareness extends Message {
	public static final int ID_STATIC = 913;

	/**
	 * Example: "lat=63.46869;lon=10.37790;d=-10"
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String depth_at_loc = "";

	/**
	 * Example: "lat=63.46869;lon=10.37790"
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String danger = "";

	public String abbrev() {
		return "ENCAwareness";
	}

	public int mgid() {
		return 913;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, depth_at_loc);
			SerializationUtils.serializePlaintext(_out, danger);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			depth_at_loc = SerializationUtils.deserializePlaintext(buf);
			danger = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
