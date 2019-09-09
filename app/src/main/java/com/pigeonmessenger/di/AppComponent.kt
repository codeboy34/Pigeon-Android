package com.pigeonmessenger.di

import com.pigeonmessenger.activities.*
import com.pigeonmessenger.fragment.GroupInfoFragment
import com.pigeonmessenger.fragment.PhotosSharedFragment
import com.pigeonmessenger.fragment.settings.BackUpFragment
import com.pigeonmessenger.job.BaseJob
import com.pigeonmessenger.services.NetworkService
import com.pigeonmessenger.viewmodals.*
import com.pigeonmessenger.webrtc.CallService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(activity: HomeActivity)

    fun inject(service: NetworkService)

    fun inject(baseJob: BaseJob)

    fun inject(messageViewModal: MessageViewModal)

    fun inject(contactsActivity: ContactsActivity)

    fun inject(dragMediaActivity: DragMediaActivity)

    fun inject(accountViewModel: AccountViewModel)

    fun inject(contactsViewModal: ContactsViewModal)

    fun inject(injector: Injector)

    fun inject(searchViewModel:SearchViewModel)

    fun inject(chatRoom: ChatRoom)

    fun inject(callService: CallService)

    fun inject(callActivity: CallActivity)

    fun inject(groupViewModel: GroupViewModel)

    fun inject(userProfileActivity: UserProfileActivity)

    fun inject(groupViewModel: GroupInfoFragment)

    fun inject(initializeActivity: InitializeActivity)

    fun inject(splashActivity: SplashActivity)

    fun inject(settingsViewModel: SettingsViewModel)

    fun inject(settingStorageViewModel: SettingStorageViewModel)

    fun inject(backUpFragment: BackUpFragment)

    fun inject(restoreActivity: RestoreActivity)

    fun inject(photosSharedFragment: PhotosSharedFragment)

    fun inject(appPreviewActivity: AvatarPreviewActivity)

    fun inject(groupInfo: GroupInfo)
}