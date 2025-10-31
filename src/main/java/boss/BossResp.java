package boss;

import lombok.Data;

@Data
public class BossResp {
    private int code;
    private String message;
    private Object zpData;
}
