package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * This message is used to send and receive Janus packets over the acoustic channel, agnostic of underlying modem.
 */
public class UamJanusPacket extends Message {
	public static final int ID_STATIC = 819;

	/**
	 * A sequence identifier that should be incremented for each
	 * request. This number will then be used to issue transmission
	 * status updates via the message UamTxStatus.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int seq = 0;

	/**
	 * Operation on the Janus packet.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public OP op = OP.values()[0];

	/**
	 * Janus baseline packet flags.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Bitfield"
	)
	public EnumSet<BASELINE_FLAGS> baseline_flags = EnumSet.noneOf(BASELINE_FLAGS.class);

	/**
	 * Time of the packet, in seconds. To be interpreted according to the flags.
	 * If the REPEAT_INTERVAL flag is set, this field indicates the time interval
	 * between packets. If the RESERVATION_TIME flag is set, sthis field indicates
	 * the time for the reservation.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "s"
	)
	public float time = 0f;

	/**
	 * Class user id.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int class_user_id = 0;

	/**
	 * Application type.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int application_type = 0;

	/**
	 * The Application Data Block corresponding to the baseline packet.
	 */
	@FieldType(
			type = IMCField.TYPE_RAWDATA
	)
	public byte[] adb = new byte[0];

	/**
	 * Human-readable error message.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String error = "";

	/**
	 * Length of Janus cargo.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int length = 0;

	/**
	 * Janus cargo, sent after the baseline packet.
	 */
	@FieldType(
			type = IMCField.TYPE_RAWDATA
	)
	public byte[] cargo = new byte[0];

	public String abbrev() {
		return "UamJanusPacket";
	}

	public int mgid() {
		return 819;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(seq);
			_out.writeByte((int)(op != null? op.value() : 0));
			long _baseline_flags = 0;
			if (baseline_flags != null) {
				for (BASELINE_FLAGS __baseline_flags : baseline_flags.toArray(new BASELINE_FLAGS[0])) {
					_baseline_flags += __baseline_flags.value();
				}
			}
			_out.writeByte((int)_baseline_flags);
			_out.writeFloat(time);
			_out.writeByte(class_user_id);
			_out.writeByte(application_type);
			SerializationUtils.serializeRawdata(_out, adb);
			SerializationUtils.serializePlaintext(_out, error);
			_out.writeByte(length);
			SerializationUtils.serializeRawdata(_out, cargo);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			seq = buf.getShort() & 0xFFFF;
			op = OP.valueOf(buf.get() & 0xFF);
			long baseline_flags_val = buf.get() & 0xFF;
			baseline_flags.clear();
			for (BASELINE_FLAGS BASELINE_FLAGS_op : BASELINE_FLAGS.values()) {
				if ((baseline_flags_val & BASELINE_FLAGS_op.value()) == BASELINE_FLAGS_op.value()) {
					baseline_flags.add(BASELINE_FLAGS_op);
				}
			}
			time = buf.getFloat();
			class_user_id = buf.get() & 0xFF;
			application_type = buf.get() & 0xFF;
			adb = SerializationUtils.deserializeRawdata(buf);
			error = SerializationUtils.deserializePlaintext(buf);
			length = buf.get() & 0xFF;
			cargo = SerializationUtils.deserializeRawdata(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum OP {
		OP_SEND_REQ(0l),

		OP_BASELINE_RECV(1l),

		OP_UNPACK_REQ(2l),

		OP_UNPACK_REPLY(3l),

		OP_UNPACK_ERROR(4l),

		OP_SEND_SUCCESS(5l),

		OP_SEND_ERROR(6l);

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

	public enum BASELINE_FLAGS {
		JANUSBL_MOBILE(0x01l),

		JANUSBL_REPEAT_INTERVAL(0x02l),

		JANUSBL_RESERVATION_TIME(0x04l),

		JANUSBL_DECODE_CAPABILITY(0x08l),

		JANUSBL_FORWARD_CAPABILITY(0x16l);

		protected long value;

		BASELINE_FLAGS(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static BASELINE_FLAGS valueOf(long value) throws IllegalArgumentException {
			for (BASELINE_FLAGS v : BASELINE_FLAGS.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for BASELINE_FLAGS: "+value);
		}
	}
}
