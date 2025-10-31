package boss;

import lombok.Data;

import java.util.List;

@Data
public class BossCity {
    private String code;
    private String name;
    private List<BossCity> subLevelModelList;
}
