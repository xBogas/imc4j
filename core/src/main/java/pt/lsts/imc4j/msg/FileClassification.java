package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * Signals that an object has been detected by any of the vehicle's sensors
 */
public class FileClassification extends Message {
	public static final int ID_STATIC = 1104;

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String original_filepath = "";

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String resized_filepath = "";

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String class_prediction = "";

	@FieldType(
			type = IMCField.TYPE_UINT8,
			max = 100
	)
	public int confidence = 0;

	/**
	 * Type of event.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public FSTYPE fstype = FSTYPE.values()[0];

	public String abbrev() {
		return "FileClassification";
	}

	public int mgid() {
		return 1104;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, original_filepath);
			SerializationUtils.serializePlaintext(_out, resized_filepath);
			SerializationUtils.serializePlaintext(_out, class_prediction);
			_out.writeByte(confidence);
			_out.writeByte((int)(fstype != null? fstype.value() : 0));
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			original_filepath = SerializationUtils.deserializePlaintext(buf);
			resized_filepath = SerializationUtils.deserializePlaintext(buf);
			class_prediction = SerializationUtils.deserializePlaintext(buf);
			confidence = buf.get() & 0xFF;
			fstype = FSTYPE.valueOf(buf.get() & 0xFF);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum FSTYPE {
		FSTYPE_IMAGE(0l),

		FSTYPE_AUDIO(1l),

		FSTYPE_SONAR(2l),

		FSTYPE_OTHER(255l);

		protected long value;

		FSTYPE(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static FSTYPE valueOf(long value) throws IllegalArgumentException {
			for (FSTYPE v : FSTYPE.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for FSTYPE: "+value);
		}
	}
}
