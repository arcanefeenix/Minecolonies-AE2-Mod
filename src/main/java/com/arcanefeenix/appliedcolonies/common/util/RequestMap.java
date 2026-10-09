package com.arcanefeenix.appliedcolonies.common.util;

import com.minecolonies.api.colony.requestsystem.token.IToken;

public class RequestMap implements Comparable {
    private IToken<?> token;
    private int amount;

    public RequestMap(IToken<?> token, int amount) {
        this.token = token;
        this.amount = amount;
    }

    public IToken<?> getToken() {
        return token;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public int compareTo(Object o) {
        RequestMap other = (RequestMap) o;
        return Integer.compare(this.amount, other.amount);
    }
}
