package com.pigeonmessenger.repo

import com.pigeonmessenger.api.AccountService
import com.pigeonmessenger.api.request.AccountRequest
import com.pigeonmessenger.api.request.AccountUpdateRequest

class AccountRepo constructor(var accountService: AccountService ){

    fun update(accountUpdateRequest: AccountUpdateRequest)= accountService.update(accountUpdateRequest)

    fun removeAvatar() = accountService.removeAvatar()

    fun login(accountRequest: AccountRequest) =accountService.verification(accountRequest)

}