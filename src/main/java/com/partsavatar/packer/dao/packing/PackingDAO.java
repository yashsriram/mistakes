package com.partsavatar.packer.dao.packing;

import com.partsavatar.packer.components.Box;
import com.partsavatar.packer.components.WarehouseOrder;

import java.io.IOException;
import java.util.List;


public interface PackingDAO {

    public List<Box> getPacking(List<Box> availableBoxes, WarehouseOrder warehouseOrder);

    public List<Box> getAvailableBoxes() throws NumberFormatException, IOException;

    public WarehouseOrder getNewOrder();

    public void storePacking(List<Box> filledBoxes);
}
