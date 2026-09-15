package com.vie.mit.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vie.mit.common.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailRideScreen(
    idRide: Int,
    viewModel: DetailRideViewModel,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết chuyến đi #$idRide") }, navigationIcon = {

            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppTheme.colors.primaryContainer,
                titleContentColor = AppTheme.colors.onPrimaryContainer
            )
            )
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... (rest of the code)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Mã chuyến đi: #$idRide",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🚗 Phương tiện: Xe điện thân thiện môi trường", fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📍 Điểm đón: 123 Nguyễn Huệ, Quận 1, TP.HCM", fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🏁 Điểm đến: Khu Công Nghệ Cao, TP. Thủ Đức", fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🌱 Lượng CO2 tiết kiệm: ~2.4 kg",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Quay lại danh sách", fontSize = 16.sp)
            }
        }
    }
}
