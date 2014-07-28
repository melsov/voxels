package voxel.landscape.collection.coordmap;

import voxel.landscape.Coord2;

import java.util.HashMap;

/**
 * Created by didyouloseyourdog on 7/28/14.
 */
public class HashCoord2D<T> {
    private Class<T> type;

    private HashMap<Coord2,T> table;

    @SuppressWarnings("unchecked")
    public HashCoord2D( Class<T> _type) {
        table = new HashMap<Coord2, T>(64*64);
        type = _type;
    }
//    public Coord2 GetSize() {
//        return null;
//    }
    public void Set(T obj, Coord2 pos) {
        table.put(pos.copy(), obj);
    }
    public void Set(T obj, int x, int y) {
        Set(obj, new Coord2(x, y));
    }

    public T GetInstance(Coord2 pos) {
        T obj = table.get(pos);
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
    public T GetInstance(int x, int y) {
        return GetInstance(new Coord2(x,y));
    }

    public T Get(Coord2 pos) {
        return table.get(pos);
    }
    public T Get(int x, int y) {
        return Get(new Coord2(x,y));
    }

    public T SafeGet(Coord2 pos) {
        return Get(pos);
    }
    public T SafeGet(int x, int y) {
        return Get(x,y);
    }

    public void AddOrReplace(T obj, Coord2 pos) {
        Set(obj, pos);
    }
    public void AddOrReplace(T obj, int x, int y) {
        AddOrReplace(obj, new Coord2(x, y));
    }
//
//    public boolean IsCorrectIndex(Coord2 pos) {
//        return IsCorrectIndex(pos.x, pos.y);
//    }
//    private boolean IsCorrectIndex(int x, int y) {
//        return true;
//    }
}
