package com.rami.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.Friend
import com.rami.online.model.FriendRequest
import com.rami.online.service.FriendService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    friendService: FriendService,
    localUid: String,
    onBack: () -> Unit
) {
    val friends  by friendService.observeFriends(localUid).collectAsState(emptyList())
    val requests by friendService.observePendingRequests(localUid).collectAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    RamiTheme {
        Column(
            Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text("👥 الأصدقاء  •  Friends",
                    color = RamiColors.Gold, fontWeight = FontWeight.Bold)
                if (requests.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Badge { Text("${requests.size}") }
                }
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("أصدقائي" to "Friends", "طلبات" to "Requests", "بحث" to "Search")
                    .forEachIndexed { i, (ar, en) ->
                        FilterChip(
                            selected = tab == i, onClick = { tab = i },
                            label = { Text("$ar  •  $en", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RamiColors.Gold,
                                selectedLabelColor     = RamiColors.DarkGreen,
                                labelColor             = RamiColors.TextLight)
                        )
                    }
            }

            when (tab) {
                0 -> FriendsList(friends, onRemove = { uid ->
                    scope.launch { friendService.removeFriend(uid) }
                })
                1 -> RequestsList(requests,
                    onAccept  = { id -> scope.launch { friendService.acceptRequest(id) } },
                    onDecline = { id -> scope.launch { friendService.declineRequest(id) } })
                2 -> SearchTab(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        if (it.length >= 3) scope.launch {
                            searchResults = friendService.searchByUsername(it)
                        } else searchResults = emptyList()
                    },
                    results = searchResults,
                    onAdd = { uid -> scope.launch { friendService.sendFriendRequest(uid) } }
                )
            }
        }
    }
}

@Composable
private fun FriendsList(friends: List<Friend>, onRemove: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(friends) { friend ->
            FriendRow(friend = friend,
                trailing = {
                    TextButton(onClick = { onRemove(friend.uid) }) {
                        Text("حذف", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                })
        }
        if (friends.isEmpty()) item {
            Text("لا يوجد أصدقاء بعد  •  No friends yet",
                color = RamiColors.TextLight.copy(alpha = 0.4f), modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun RequestsList(
    requests: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(requests) { req ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RamiColors.TextLight.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(req.fromUsername, color = RamiColors.TextLight, fontWeight = FontWeight.Medium)
                    Row {
                        TextButton(onClick = { onAccept(req.id) }) {
                            Text("قبول  •  Accept", color = RamiColors.Gold, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDecline(req.id) }) {
                            Text("رفض  •  Decline", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (requests.isEmpty()) item {
            Text("لا توجد طلبات  •  No pending requests",
                color = RamiColors.TextLight.copy(alpha = 0.4f), modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Friend>,
    onAdd: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            label = { Text("ابحث باسم المستخدم  •  Search username") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = RamiColors.Gold,
                unfocusedBorderColor = RamiColors.TextLight.copy(alpha = 0.3f),
                focusedLabelColor    = RamiColors.Gold,
                focusedTextColor     = RamiColors.TextLight,
                unfocusedTextColor   = RamiColors.TextLight
            )
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { friend ->
                FriendRow(friend = friend, trailing = {
                    TextButton(onClick = { onAdd(friend.uid) }) {
                        Text("إضافة  •  Add", color = RamiColors.Gold, fontSize = 12.sp)
                    }
                })
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, trailing: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RamiColors.TextLight.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(RamiColors.Gold.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(if (friend.isOnline) "🟢" else "⚫", fontSize = 10.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.username, color = RamiColors.TextLight, fontWeight = FontWeight.Medium)
                Text("⭐ ${friend.rating}", color = RamiColors.TextLight.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            trailing()
        }
    }
}
