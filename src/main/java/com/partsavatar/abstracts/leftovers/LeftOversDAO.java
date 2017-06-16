package com.partsavatar.abstracts.leftovers;

import com.partsavatar.abstracts.SInequality;

import java.util.Vector;

public interface LeftOversDAO {

    public Vector<SInequality> getAll();

    public boolean clearTableAndSave(final Vector<SInequality> partEstimates);

}
