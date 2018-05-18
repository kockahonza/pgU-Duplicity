import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DuplicityTest {
	@Test
	public void NoInput() {
		Duplicity.main(new String[]{""});
	}
	@Test
	public void SimpleInput() {
		Duplicity.main(new String[]{"sample_inputtt"});
	}
}
