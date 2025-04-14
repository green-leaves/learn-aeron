package oms;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    private long orderId;
    private long clientId;
    private String ccyPair;
    private boolean isBid;
    private double targetPrice;
}
