package com.ruru.practice.feature.practice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable fun RootProtectionScreen(viewModel: RootProtectionViewModel=hiltViewModel(), modifier: Modifier = Modifier){
 val s by viewModel.state.collectAsState()
 LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Spacer(Modifier.height(20.dp));Text("护根",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("白天遇到色、声、香、味、触、法时，知道接触与随后发展的受、爱、取。",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("眼","耳","鼻","舌","身","意").forEach{FilterChip(s.sense==it,{viewModel.sense(it)},label={Text(it)})}}}
  item{Field("接触","${s.contact}",viewModel::contact,"发生了什么接触？")}
  item{Field("受",s.feeling,viewModel::feeling,"苦、乐或不苦不乐？")}
  item{Field("爱",s.craving,viewModel::craving,"心想继续得到、排斥或改变什么？")}
  item{Field("取",s.grasping,viewModel::grasping,"正在抓住哪个故事、立场或对象？")}
  item{Field("回应",s.response,viewModel::response,"在哪一步停一下？准备怎样回应？",3)}
  item{Button(onClick=viewModel::save,enabled=!s.saving,modifier=Modifier.fillMaxWidth()){Text(if(s.saving)"保存中…" else "保存护根观察")};s.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}}
  item{Text("最近记录",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)}
  items(s.history,key={it.id}){h->Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text("${h.date} · ${h.sense}",fontWeight=FontWeight.SemiBold);Text("触：${h.contact}");Text("受：${h.feeling} · 爱：${h.craving}");Text("取：${h.grasping}");if(h.response.isNotBlank())Text("回应：${h.response}",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
  item{Spacer(Modifier.height(24.dp))}
 }
}
@Composable private fun Field(label:String,value:String,onValueChange:(String)->Unit,placeholder:String,minLines:Int=2)=OutlinedTextField(value,onValueChange,label={Text(label)},placeholder={Text(placeholder)},minLines=minLines,modifier=Modifier.fillMaxWidth())
