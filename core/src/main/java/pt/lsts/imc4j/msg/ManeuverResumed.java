package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * This message is sent when a maneuver is stoped, describing how it could be resumed to completion later.
 */
public class ManeuverResumed extends Message {
	public static final int ID_STATIC = 2020;

	/**
	 * ID of the maneuver that was stopped.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String man_id = "";

	/**
	 * List of maneuvers that can be executed to resume the stopped maneuver.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGELIST
	)
	public ArrayList<Maneuver> man_list = new ArrayList<>();

	public String abbrev() {
		return "ManeuverResumed";
	}

	public int mgid() {
		return 2020;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, man_id);
			SerializationUtils.serializeMsgList(_out, man_list);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			man_id = SerializationUtils.deserializePlaintext(buf);
			man_list = SerializationUtils.deserializeMsgList(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
