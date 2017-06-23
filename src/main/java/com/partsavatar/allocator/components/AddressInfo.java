package com.partsavatar.allocator.components;

import com.easypost.model.Address;
import com.partsavatar.allocator.api.easyPost.EasyPostAPI;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode(of = {"raw"})
@ToString
public class AddressInfo {
    @NonNull
    private String raw;
    private Address easyPostAddress;

    public AddressInfo(String raw) {
        this.raw = raw;
        this.initEasyPostAddress(null);
    }

    public void initEasyPostAddress(String id) {
        String[] address = raw.split(",");
        String[] state = address[2].split("-");
        try {
            easyPostAddress = EasyPostAPI.getAddress(id, address[0], address[1], state[0], state[1]);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Wrong Syntax of address");
        }
    }

    public AddressInfo(AddressInfo address) {
        this.raw = address.raw;
        if (address.easyPostAddress.equals(null))
            this.initEasyPostAddress(null);
        else
        	this.easyPostAddress = address.easyPostAddress;
    }

}
