package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Notification {
    String content;
    String title;
    String amount;
}
