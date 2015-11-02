import java.util.Comparator;
import java.util.LinkedList;

public class customComparator implements Comparator<LinkedList> {


	@Override
	public int compare(LinkedList o1, LinkedList o2) {
		return ((Integer) o1.size()).compareTo(o2.size());
	}

}
