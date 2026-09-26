package com.ruru.practice.feature.practice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.data.entity.RootProtectionEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class RootProtectionUiState(val sense:String="眼",val contact:String="",val feeling:String="",val craving:String="",val grasping:String="",val response:String="",val saving:Boolean=false,val message:String?=null,val history:List<RootProtectionEntity> = emptyList())
@HiltViewModel class RootProtectionViewModel @Inject constructor(private val repo:PracticeRepository):ViewModel(){
 private val _state=MutableStateFlow(RootProtectionUiState());val state=_state.asStateFlow();init{refresh()}
 fun sense(v:String){_state.value=_state.value.copy(sense=v)};fun contact(v:String){_state.value=_state.value.copy(contact=v)};fun feeling(v:String){_state.value=_state.value.copy(feeling=v)};fun craving(v:String){_state.value=_state.value.copy(craving=v)};fun grasping(v:String){_state.value=_state.value.copy(grasping=v)};fun response(v:String){_state.value=_state.value.copy(response=v)}
 fun save(){val s=_state.value;if(s.contact.isBlank())return;viewModelScope.launch{_state.value=s.copy(saving=true);repo.saveRootProtection(RootProtectionEntity(date=now(),sense=s.sense,contact=s.contact.trim(),feeling=s.feeling.trim(),craving=s.craving.trim(),grasping=s.grasping.trim(),response=s.response.trim()));_state.value=RootProtectionUiState(history=repo.getRecentRootProtections(),message="已保存护根观察")}}
 private fun refresh(){viewModelScope.launch{_state.value=_state.value.copy(history=repo.getRecentRootProtections())}};private fun now()=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date())
}
