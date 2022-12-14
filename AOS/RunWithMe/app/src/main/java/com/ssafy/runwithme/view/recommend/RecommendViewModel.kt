package com.ssafy.runwithme.view.recommend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.runwithme.model.dto.CoordinateDto
import com.ssafy.runwithme.model.dto.TrackBoardFileDto
import com.ssafy.runwithme.model.response.RecommendResponse
import com.ssafy.runwithme.repository.CrewRepository
import com.ssafy.runwithme.repository.RecommendRepository
import com.ssafy.runwithme.utils.Result
import com.ssafy.runwithme.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import javax.inject.Inject

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val recommendRepository: RecommendRepository,
    private val crewRepository: CrewRepository
): ViewModel() {

    private val _successMsgEvent = SingleLiveEvent<String>()
    val successMsgEvent get() = _successMsgEvent

    private val _errorMsgEvent = SingleLiveEvent<String>()
    val errorMsgEvent get() = _errorMsgEvent

    private val _trackBoardList : MutableStateFlow<List<TrackBoardFileDto>>
            = MutableStateFlow(listOf())
    val trackBoardList get() = _trackBoardList.asStateFlow()

    private val _getCoordinates : MutableStateFlow<List<CoordinateDto>> = MutableStateFlow(listOf())
    val getCoordinates get() = _getCoordinates

    fun createRecommend(trackBoardDto: RequestBody, img: MultipartBody.Part) {
        viewModelScope.launch(Dispatchers.IO) {
            recommendRepository.createRecommend(trackBoardDto, img)
                .collectLatest {
                    if (it is Result.Success) {
                        _successMsgEvent.postValue(it.data.msg)
                    } else if (it is Result.Fail) {
                        _errorMsgEvent.postValue(it.data.msg)
                    }
                }
        }
    }

    fun getRecommends(leftLng: Double, lowerLat: Double, rightLng: Double, upperLat: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            recommendRepository.getRecommends(leftLng, lowerLat, rightLng, upperLat).collectLatest {
                if (it is Result.Success) {
                    Log.d("test5", "getRecommends: $it")
                    _trackBoardList.value = it.data.data
                } else if (it is Result.Fail) {
                    _errorMsgEvent.postValue(it.data.msg)
                }
            }
        }
    }

    fun getCoordinates(recordSeq: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            crewRepository.getCoordinates(recordSeq).collectLatest {
                Log.d("test5", "getCoordinates: $it")
                if(it is Result.Success){
                    _getCoordinates.value = it.data.data
                }else if(it is Result.Fail){
                    _errorMsgEvent.postValue(it.data.msg)
                }
            }
        }
    }
}