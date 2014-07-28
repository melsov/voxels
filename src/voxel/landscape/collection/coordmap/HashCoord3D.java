package voxel.landscape.collection.coordmap;

import voxel.landscape.Coord3;

import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 7/28/14.
 */
public class HashCoord3D <T>
{
    private Class<T> type;

    private HashMap<Coord3,T> table;

//    private Coord3 lastCoord = new Coord3(Integer.MAX_VALUE, Integer.MAX_VALUE, -23);
//    private T lastObj = null;

    @SuppressWarnings("unchecked")
    public HashCoord3D(Class<T> _type) {
        table = new HashMap<Coord3, T>(64*64);
        type = _type;
    }
//    public Coord3 GetSize() {
//        return null;
//    }
    public void Set(T obj, Coord3 pos) {
//        lastCoord = pos.copy();
//        lastObj = obj;
        table.put(pos.copy(), obj);
    }
    public void Set(T obj, int x, int y, int z) {
        Set(obj, new Coord3(x, y, z));
    }

    public T GetInstance(Coord3 pos) {
        T obj = Get(pos);
        if (obj == null) {
            try {
                obj = type.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            Set(obj, pos);
        }
        return obj;
    }
    public T GetInstance(int x, int y, int z) {
        return GetInstance(new Coord3(x,y, z));
    }

    public T Get(Coord3 pos) {
//        if (pos.equal(lastCoord)) {
//            return lastObj;
//        }
//        lastCoord = pos;
//        lastObj = table.get(pos);
        return table.get(pos); // lastObj;
    }
    public T Get(int x, int y, int z) {
        return Get(new Coord3(x, y, z));
    }

    public T SafeGet(Coord3 pos) {
        return Get(pos);
    }
    public T SafeGet(int x, int y, int z) {
        return Get(x,y, z);
    }

    public void AddOrReplace(T obj, Coord3 pos) {
        Set(obj, pos);
    }
    public void AddOrReplace(T obj, int x, int y, int z) {
        AddOrReplace(obj, new Coord3(x, y, z));
    }

//    public boolean IndexWithinBounds(Coord3 pos) {
//        return IndexWithinBounds(pos.x, pos.y, pos.z);
//    }
//    private boolean IndexWithinBounds(int x, int y, int z) {
//        return
//    }
}
