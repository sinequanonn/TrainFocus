package trainfocus.backend.room.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorTest {
    private final CodeGenerator codeGenerator = new CodeGenerator();

    @Test
    void 코드는_8자리이고_대문자_숫자로만_구성() {
        String code = codeGenerator.issue();

        assertThat(code).hasSize(8);
        assertThat(code).matches("[A-Z0-9]{8}");
    }

    @Test
    void 여러_번_호출해도_서로_다른_코드_생성() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(codeGenerator.issue());
        }

        assertThat(codes).hasSize(100);
    }
}
