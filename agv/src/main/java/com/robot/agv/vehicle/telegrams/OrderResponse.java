/**
 * Copyright (c) Fraunhofer IML
 */
package com.robot.agv.vehicle.telegrams;

import static com.google.common.base.Ascii.ETX;
import static com.google.common.base.Ascii.STX;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.primitives.Ints;
import com.robot.agv.common.telegrams.Response;

import static java.util.Objects.requireNonNull;

/**
 * Represents an order response sent from the vehicle.
 *
 * @author Martin Grzenia (Fraunhofer IML)
 */
public class OrderResponse
    extends Response {

  /**
   * The response type.
   */
  public static final byte TYPE = 2;
  /**
   * The expected length of a telegram of this type.
   */
  public static final int TELEGRAM_LENGTH = 9;
  /**
   * The size of the payload (the raw content, without STX, SIZE, CHECKSUM and ETX).
   */
  public static final int PAYLOAD_LENGTH = TELEGRAM_LENGTH - 4;
  /**
   * The position of the checksum byte.
   */
  public static final int CHECKSUM_POS = TELEGRAM_LENGTH - 2;
  /**
   * The order id received by the vehicle.
   */
  private int orderId;

  /**
   * Creates a new instance.
   *
   * @param telegramData This telegram's raw content.
   */
  public OrderResponse(String telegramData) {
    super(TELEGRAM_LENGTH);
    requireNonNull(telegramData, "telegramData");
    checkArgument(telegramData.length() == TELEGRAM_LENGTH);

    System.arraycopy(telegramData, 0, rawContent, 0, TELEGRAM_LENGTH);
    decodeTelegramContent();
  }

  /**
   * Returns the order id received by the vehicle.
   *
   * @return The order id received by the vehicle.
   */
  public int getOrderId() {
    return orderId;
  }

  @Override
  public String toString() {
    return "OrderResponse{" + "id=" + id + '}';
  }

  /**
   * 检查是否为状态类型的响应
   *
   * @param protocol 待检查的报文协议对象
   * @return 是则返回true，否则返回false
   */
  public static boolean isOrderResponse(Protocol protocol) {
    requireNonNull(protocol, "报文协议对象不能为空");

    boolean result = true;

    return result;
  }

  private void decodeTelegramContent() {
    this.id = Ints.fromBytes((byte) 0, (byte) 0, rawContent[3], rawContent[4]);
    orderId = Ints.fromBytes((byte) 0, (byte) 0, rawContent[5], rawContent[6]);
  }
}