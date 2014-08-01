package voxel.landscape.util;

public class Asserter {
	public static void assertTrue(boolean condition) {
		assertTrue(condition, "");
	}
	public static void assertFalseAndDie(String s) {
		assertTrue(false, s);
	}
	public static void assertTrue(boolean condition, String s) {
		if (!condition) {
			System.out.println(s);
			try {
				throw new Exception(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
            System.exit(1);
		}
	}
}
