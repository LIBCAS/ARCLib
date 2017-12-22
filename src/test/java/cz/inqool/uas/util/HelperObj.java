package cz.inqool.uas.util;

import lombok.*;

public class HelperObj {

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class A {
        private String a;
        private Integer b;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class B {
        private String aa;
        private Integer bb;
        private Double cc;
    }
}