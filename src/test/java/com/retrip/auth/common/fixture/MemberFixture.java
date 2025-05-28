package com.retrip.auth.common.fixture;

import com.retrip.auth.application.in.request.LoginRequest;

public abstract class MemberFixture {
    public static LoginRequest loginRequest(String email, String password){
        return new LoginRequest(email,password);
    }
}
