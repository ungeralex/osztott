package hu.elte;

public enum AgencyEnum {
    FIRST("FIRST"),
    SECOND("SECOND");

    private String text;

    AgencyEnum(String text) {
        this.text = text;
    }

//    public AgencyEnum getEnumFromString(String text) {
//        AgencyEnum result = null;
//
//        switch (text) {
//            case "FIRST":
//                result = FIRST;
//                break;
//            case "SECOND":
//                result = SECOND;
//                break;
//        }
//
//        return result;
//    }

    @Override
    public String toString() {
        return this.text;
    }
}
