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
 * This message is used to describe the ValuesIf content of a TypedEntityParameter.
 */
public class ValuesIf extends Message {
	public static final int ID_STATIC = 2018;

	/**
	 * Name of parameter to compare
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String param = "";

	/**
	 * Value to compare
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String value = "";

	/**
	 * List of possible values if param=value
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String values_list = "";

	public String abbrev() {
		return "ValuesIf";
	}

	public int mgid() {
		return 2018;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, param);
			SerializationUtils.serializePlaintext(_out, value);
			SerializationUtils.serializePlaintext(_out, values_list);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			param = SerializationUtils.deserializePlaintext(buf);
			value = SerializationUtils.deserializePlaintext(buf);
			values_list = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
