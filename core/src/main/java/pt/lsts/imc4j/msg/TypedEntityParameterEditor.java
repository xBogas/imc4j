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
public class TypedEntityParameterEditor extends TypedEntityParametersOptions {
	public static final int ID_STATIC = 2036;

	/**
	 * String name of the editor to be used for this entity parameters.
	 * This value is advisory only. The receiving end should try to honor it,
	 * for the edition respect more complicated logic that Values-If can provide.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String value = "";

	public String abbrev() {
		return "TypedEntityParameterEditor";
	}

	public int mgid() {
		return 2036;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, value);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			value = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
