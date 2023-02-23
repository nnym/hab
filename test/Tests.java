import hab.Hab;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import java.util.stream.IntStream;

@Testable
public class Tests {
	@Test void test() {
		var hab = new Hab<Object, String>();
		IntStream.range(0, 26).forEach(n -> hab.map(n, String.valueOf((char) ('A' + n))));

		hab.map(1, "A");
		hab.map("b", "B");
		hab.map(Hab.class, "C");
		hab.map(true, "D");
		hab.map(0, "E");

		hab.map("b", "BBB");
		hab.unmap(Hab.class);
		hab.map(2, "A");
		hab.unmap(2);
		var bp = true;
	}
}
