package cz.inqool.uas.index.dto;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DtoUtils {
    public static <T, U> Result<U> transform(Result<T> result, Function<T, U> mapper) {
        List<U> items = result.getItems().stream()
                              .map(mapper)
                              .collect(Collectors.toList());

        return new Result<>(items, result.getCount());
    }
}
