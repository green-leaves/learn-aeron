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
    private double targetPrice;
    private boolean isBid;
    private String ccyPair;
}
