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
 * This message is used by the receiver of MessageParts messages
 * to inform the sender of the status of the reception of a message
 * in fragments.
 * The sender can then use this information to determine which
 * fragments were received and which ones were not.
 * This message is sent in response to a MessagePart message.
 */
public class MessagePartControl extends Message {
	public static final int ID_STATIC = 878;

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int uid = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public OP op = OP.values()[0];

	/**
	 * Comma-separated list of fragment numbers. Example: "1,2,3".
	 * This field is used to inform the sender of the fragments that
	 * should be considered. If this field starts with '!', it means
	 * that the indicated fragments should not be considered. Example:
	 * "!1,2,3" means that all fragments should be considered except
	 * 1,2,3. With this field equal to only "!" it means that all
	 * fragments must be considered.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String frag_ids = "";

	public String abbrev() {
		return "MessagePartControl";
	}

	public int mgid() {
		return 878;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte(uid);
			_out.writeByte((int)(op != null? op.value() : 0));
			SerializationUtils.serializePlaintext(_out, frag_ids);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			uid = buf.get() & 0xFF;
			op = OP.valueOf(buf.get() & 0xFF);
			frag_ids = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum OP {
		OP_STATUS_RECEIVED(0l),

		OP_REQUEST_RETRANSMIT(1l);

		protected long value;

		OP(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static OP valueOf(long value) throws IllegalArgumentException {
			for (OP v : OP.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for OP: "+value);
		}
	}
}
