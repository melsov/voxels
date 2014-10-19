package voxel.landscape.coord;

import com.jme3.math.Vector3f;

public class Coord3 implements ICoordXZ
{
	public int x,y,z;
	
	public static final Coord3 xpos = new Coord3(1,0,0);
	public static final Coord3 ypos = new Coord3(0,1,0);
	public static final Coord3 zpos = new Coord3(0,0,1);
	
	public static final Coord3 xneg = new Coord3(-1,0,0);
	public static final Coord3 yneg = new Coord3(0,-1,0);
	public static final Coord3 zneg = new Coord3(0,0,-1);
	
	public static final Coord3 right = xpos;
	public static final Coord3 up = ypos;
	public static final Coord3 forward = zpos;

	public Coord3(int _x, int _y, int _z)
	{
		x = _x; y = _y; z = _z;
	}

	public Coord3(int a) { this(a,a,a); }
	public Coord3(double _x, double _y, double _z) { this((int) _x, (int) _y,(int) _z); }

	public Coord3 multy(Coord3 other) {
		return new Coord3(this.x * other.x, this.y * other.y, this.z * other.z);
	}
	public Coord3 multy(int i) {
		return new Coord3(this.x * i, this.y * i, this.z * i);
	}
	public Coord3 multy(Vector3f other)
	{
		return new Coord3(this.x * other.x, this.y * other.y, this.z * other.z);
	}
	public Coord3 divideBy(Coord3 other) {
		return new Coord3(x/other.x, y/other.y, z/other.z);
	}
	public Coord3 divideBy(double other) {
		return new Coord3(x/other, y/other, z/other);
	}
	public Coord3 add(Coord3 other) {
		return new Coord3(this.x + other.x, this.y + other.y, this.z + other.z);		
	}
	public Coord3 add(int i) {
		return new Coord3(this.x + i, this.y + i, this.z + i);
	}
	public Coord3 minus (Coord3 other) {
		return new Coord3(this.x - other.x, this.y - other.y, this.z - other.z);		
	}

    @Override
	public Coord3 clone() {
		return new Coord3(this.x, this.y, this.z);
	}
	
	public static Coord3 ZeroFlipsToOneNonZeroFlipsToZero(Coord3 coord) {
		Coord3 co = coord.clone();
		co.x = co.x != 0 ?  0 : 1;
		co.y = co.y != 0 ?  0 : 1;
		co.z = co.z != 0 ?  0 : 1;
		return co;
	}
	public static Coord3 Min (Coord3 a, Coord3 b) {
		return new Coord3(a.x < b.x ? a.x : b.x , a.y < b.y ? a.y : b.y , a.z < b.z ? a.z : b.z);
	}
	public static Coord3 Max (Coord3 a, Coord3 b) {
		return new Coord3(a.x > b.x ? a.x : b.x , a.y > b.y ? a.y : b.y , a.z > b.z ? a.z : b.z);
	}
	public boolean greaterThan(Coord3 other) {
		return x > other.x && y > other.y && z > other.z;
	}
    public boolean greaterThanOrEqual(Coord3 other) {
        return x >= other.x && y >= other.y && z >= other.z;
    }
	public boolean lessThan(Coord3 other) {
		return x < other.x && y < other.y && z < other.z;
	}
	public double distanceSquared() {
		return x*x + y*y;
	}
    public double distanceSquared(Coord3 other) {
        return this.minus(other).distanceSquared();
    }
	public Coord3 sign() {
		return new Coord3(Math.signum(x), Math.signum(y), Math.signum(z));
	}
    public Coord3 signNonZero() { return new Coord3(x < 0 ? -1 : 1, y < 0 ? -1 : 1, z < 0 ? -1 : 1 ); }
	public static Coord3 Zero = new Coord3(0,0,0); 
	public static Coord3 One = new Coord3(1,1,1); 
	
	public Vector3f toVector3()
	{
		return new Vector3f(this.x, this.y, this.z);
	}
	public static Coord3 FromVector3f(Vector3f v) { return new Coord3(v.x, v.y, v.z); }

    public static Coord3 FromVector3fAdjustNegative(Vector3f v) { return Coord3.FromVector3f(VektorUtil.SubtractOneFromNegativeComponents(v)); }

    public static Coord3 GreatestDirectionCoord(Vector3f dir) {
    	Coord3 res = dir.x < 0 ? Coord3.xneg : Coord3.xpos;
    	if (Math.abs(dir.y) > Math.abs(dir.x) && Math.abs(dir.y) > Math.abs(dir.z)) {
    		res = dir.y < 0 ? Coord3.yneg : Coord3.ypos;
    	}
    	if (Math.abs(dir.z) > Math.abs(dir.x)) {
    		res = dir.z < 0 ? Coord3.zneg : Coord3.zpos;
    	}
    	return res;
    }
    public static Coord3 FlipZeroToOneMask(Coord3 c) {
        return new Coord3(c.x == 0 ? 1 : 0,c.y == 0 ? 1 : 0,c.z == 0 ? 1 : 0 );
    }
    
	@Override
	public String toString() { return String.format("Coord3 x: %d, y: %d, z: %d", x,y,z); }

    @Override
    public boolean equals(Object other) {
        if ( !(other instanceof Coord3))  return false;
        if (other == this) return true;
        return this.equal((Coord3) other);
    }
    public boolean equal(Coord3 other) {
        return x==other.x && y==other.y && z==other.z;
    }

    /* hashCode method that's tailored to collection classes in the voxel engine.
     * I.e. collections of blocks in a chunk (local coords) and collections of
     * chunks (chunk coords). DOES NOT work for collections of global block coords
     * but non exist in this program.
     * chunk y ranges btwn -1 and 16, local block coords x,y,z are between -1 and 16. so y
     * never needs more than 18 (-1 through 16) values which we can cover with 5 bits (2^5 = 32)
     * so 'skimp' appropriately on y's bits... (only need first 5)
     */
    @Override
    public int hashCode() {
        return (z & Integer.MIN_VALUE) | ((y & Integer.MIN_VALUE) >>> 1 ) | ((x & Integer.MIN_VALUE) >>> 2 ) |
                ((z & 4095) << 17) | ((y & 31) << 12) | (x & 4095);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }
}
