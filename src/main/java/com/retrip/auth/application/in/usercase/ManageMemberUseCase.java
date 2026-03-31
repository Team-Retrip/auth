package com.retrip.auth.application.in.usercase;

import com.retrip.auth.application.in.request.*;
import com.retrip.auth.application.in.response.*;

import java.util.List;
import java.util.UUID;

public interface ManageMemberUseCase {
    MemberCreateResponse createUser(MemberCreateRequest request);

    MemberUpdateResponse updateUser(UUID memberId, MemberUpdateRequest request);
    void deleteUser(UUID memberId, MemberDeleteRequest request);
    ChangePasswordResponse changePassword(UUID memberId, ChangePasswordRequest request);

    MemberInfoResponse getMyInfo(UUID memberId);
    VerifyPasswordResponse verifyPassword(UUID memberId, VerifyPasswordRequest request);

    UUID findIdByEmail(String email);

    List<MemberSearchResponse> searchMembers(String name);

    List<MemberSearchResponse> getMembersByIds(List<UUID> ids);
}