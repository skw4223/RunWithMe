package com.ssafy.runwithme.view.login.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.ssafy.runwithme.MainActivity
import com.ssafy.runwithme.R
import com.ssafy.runwithme.base.BaseFragment
import com.ssafy.runwithme.databinding.FragmentUserLoginBinding
import com.ssafy.runwithme.model.dto.FcmTokenDto
import com.ssafy.runwithme.view.login.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserLoginFragment : BaseFragment<FragmentUserLoginBinding>(R.layout.fragment_user_login) {

    private val userViewModel by activityViewModels<UserViewModel>()

    override fun init() {
        binding.userVm = userViewModel

        initClickListener()

        initViewModelCallBack()
    }

    private fun initClickListener(){
        binding.apply {
            // TEST
            imgLogo.setOnClickListener {
                startActivity(Intent(requireContext(),MainActivity::class.java))
                requireActivity().finish()
            }
            cardLoginGoogle.setOnClickListener {
                googleSignIn()
            }
            cardLoginNaver.setOnClickListener {
                naverSignIn()
            }
            cardLoginKakao.setOnClickListener {
                kakaoSignIn()
            }
        }
    }

    private fun initViewModelCallBack(){
        userViewModel.joinEvent.observe(viewLifecycleOwner) {
            val action = UserLoginFragmentDirections.actionUserLoginFragmentToUserJoinFragment(it)
            findNavController().navigate(action)
        }
        userViewModel.loginEvent.observe(viewLifecycleOwner){
            showToast(it)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if(!task.isSuccessful){
                    return@addOnCompleteListener
                }else{
                    userViewModel.fcmToken(FcmTokenDto(task.result))
                }
            }
        }
        userViewModel.fcmEvent.observe(viewLifecycleOwner){
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
        userViewModel.errorMsgEvent.observe(viewLifecycleOwner){
            showToast(it)
        }
        userViewModel.joinMsgEvent.observe(viewLifecycleOwner){
            showToast(it)
        }
    }

    private fun googleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.EMAIL))
            .requestServerAuthCode(resources.getString(R.string.google_client_key))
            .requestEmail()
            .requestIdToken(getString(R.string.google_client_key))
            .build()

        val mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        googleSignInResult.launch(signInIntent)
    }

    private val googleSignInResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {

        if (it.resultCode == Activity.RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val accessToken = account.idToken!!

                userViewModel.googleLogin(accessToken)
            } catch (e: ApiException) {
                Log.w("test5", "signInResult:failed code=" + e.statusCode)
            }
        } else {
            Log.d("test5", "$it: ")
        }
    }

    val TAG = "test5"
    private fun kakaoSignIn(){
        Log.e(TAG, "?????????1")
        // ????????????????????? ????????? ?????? callback ??????
        // ?????????????????? ????????? ??? ??? ?????? ????????????????????? ???????????? ?????? ?????????
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            Log.e(TAG, "?????????8")
            if (error != null) {
                Log.e(TAG, "?????????2")
                Log.e(TAG, "????????????????????? ????????? ??????", error)
            } else if (token != null) {
                Log.e(TAG, "?????????3")
                Log.i(TAG, "????????????????????? ????????? ?????? ${token.accessToken}")

                userViewModel.kakaoLogin(token.accessToken)

                // ????????? ?????? ?????? (??????)
                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        Log.e(TAG, "?????????4")
                        Log.e(TAG, "????????? ?????? ?????? ??????", error)
                    }
                    else if (user != null) {
                        Log.e(TAG, "?????????5")
                        Log.i(TAG, "????????? ?????? ?????? ??????" +
                                "\n????????????: ${user.id}" +
                                "\n?????????: ${user.kakaoAccount?.email}" +
                                "\n?????????: ${user.kakaoAccount?.profile?.nickname}")
                    }else{
                        Log.e(TAG, "?????????6")
                    }
                }
            }else{
                Log.e(TAG, "?????????7")
            }
        }

        // ??????????????? ???????????? ????????? ?????????????????? ?????????, ????????? ????????????????????? ?????????
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
            UserApiClient.instance.loginWithKakaoTalk(requireContext()) { token, error ->
                if (error != null) {
                    Log.e(TAG, "?????????????????? ????????? ??????", error)

                    // ???????????? ???????????? ?????? ??? ???????????? ?????? ?????? ???????????? ???????????? ????????? ??????,
                    // ???????????? ????????? ????????? ?????? ????????????????????? ????????? ?????? ?????? ????????? ????????? ?????? (???: ?????? ??????)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }
                    // ??????????????? ????????? ?????????????????? ?????? ??????, ????????????????????? ????????? ??????
                    UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
                } else if (token != null) {
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e(TAG, "????????? ?????? ?????? ??????", error)
                        }
                        else if (user != null) {
                            Log.i(TAG, "????????? ?????? ?????? ??????" +
                                    "\n????????????: ${user.id}" +
                                    "\n?????????: ${user.kakaoAccount?.email}" +
                                    "\n?????????: ${user.kakaoAccount?.profile?.nickname}")
                        }
                    }
                    Log.i(TAG, "?????????????????? ????????? ?????? ${token.accessToken}")

                    userViewModel.kakaoLogin(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
        }
    }


    private fun naverSignIn(){
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onSuccess(result: NidProfileResponse) {
                        val email = result.profile?.email.toString()
                        Log.d(TAG, "onSuccess: ${result.profile}")
                        Log.d(TAG, "onSuccess: $email")

                        Log.d(TAG, "onSuccess: ${NaverIdLoginSDK.getAccessToken()!!}")
                        userViewModel.naverLogin(NaverIdLoginSDK.getAccessToken()!!, email)
                    }
                    override fun onError(errorCode: Int, message: String) {}
                    override fun onFailure(httpStatus: Int, message: String) {}
                })
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                showToast("errorCode:$errorCode, errorDesc:$errorDescription")
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }

        NaverIdLoginSDK.authenticate(requireContext(), oauthLoginCallback)
    }
}