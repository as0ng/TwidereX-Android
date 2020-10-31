package com.twidere.twiderex.scenes

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRowForIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.node.Ref
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.twidere.twiderex.component.*
import com.twidere.twiderex.extensions.navViewModel
import com.twidere.twiderex.extensions.withElevation
import com.twidere.twiderex.maxComposeTextLength
import com.twidere.twiderex.model.ui.UiStatus
import com.twidere.twiderex.ui.AmbientNavController
import com.twidere.twiderex.ui.composeImageSize
import com.twidere.twiderex.ui.profileImageSize
import com.twidere.twiderex.ui.standardPadding
import com.twidere.twiderex.utils.AmbientLauncher
import com.twidere.twiderex.viewmodel.ActiveAccountViewModel
import com.twidere.twiderex.viewmodel.ComposeViewModel
import kotlinx.coroutines.launch


enum class ComposeType {
    New,
    Reply,
    Quote,
}

@Composable
fun ComposeScene(status: ComposeType, composeType: String?) {
    //TODO: implementation
}

@OptIn(ExperimentalLazyDsl::class, ExperimentalFocus::class, ExperimentalFoundationApi::class)
@Composable
fun ComposeScene(status: UiStatus? = null, composeType: ComposeType = ComposeType.New) {
    val viewModel = navViewModel<ComposeViewModel>()
    val (text, setText) = remember { mutableStateOf("") }
    val activeAccountViewModel = navViewModel<ActiveAccountViewModel>()
    val images by viewModel.images.observeAsState(initial = emptyList())
    val account by activeAccountViewModel.account.observeAsState()
    val location by viewModel.location.observeAsState()
    val locationEnabled by viewModel.locationEnabled.observeAsState(initial = false)
    val navController = AmbientNavController.current
    val keyboardController = remember { Ref<SoftwareKeyboardController>() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (status == null) {
            0
        } else {
            1
        }
    )
    status?.also {
        if (listState.firstVisibleItemIndex == 0) {
            keyboardController.value?.hideSoftwareKeyboard()
        } else if (listState.firstVisibleItemIndex == 1) {
            keyboardController.value?.showSoftwareKeyboard()
        }
    }
    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = when (composeType) {
                            ComposeType.Reply -> "Reply"
                            ComposeType.Quote -> "Quote"
                            else -> "Compose"
                        }
                    )
                },
                navigationIcon = {
                    AppBarNavigationButton(icon = Icons.Default.Close)
                },
                actions = {
                    IconButton(
                        enabled = text.isNotEmpty(),
                        onClick = {
                            viewModel.compose(text, composeType, status)
                            navController.popBackStack()
                        }
                    ) {
                        Icon(asset = Icons.Default.Send)
                    }
                }
            )
        }
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1F),
                state = listState,
            ) {
                status?.let { status ->
                    item {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colors.surface.withElevation())
                        ) {
                            StatusLineComponent(lineDown = true) {
                                TimelineStatusComponent(
                                    data = status,
                                    showActions = false,
                                )
                            }
                        }
                    }
                }
                item {
                    StatusLineComponent(
                        lineUp = status != null,
                    ) {
                        Row(
                            modifier = Modifier.fillParentMaxSize()
                                .padding(16.dp),
                        ) {
                            account?.let {
                                NetworkImage(
                                    url = it.user.profileImage,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .width(profileImageSize)
                                        .height(profileImageSize)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier.weight(1F)
                            ) {
                                TextInput(
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    value = text,
                                    onValueChange = { setText(it) },
                                    autoFocus = true,
                                    onTextInputStarted = {
                                        keyboardController.value = it
                                    },
                                    onClicked = {
                                        // TODO: scroll lazyColumn
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (images.any()) {
                LazyRowForIndexed(
                    modifier = Modifier.padding(horizontal = standardPadding * 2),
                    items = images
                ) { index, item ->
                    ComposeImage(item)
                    if (index != images.lastIndex) {
                        Spacer(modifier = Modifier.width(standardPadding))
                    }
                }
                Spacer(modifier = Modifier.height(standardPadding * 2))
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = standardPadding * 2)
            ) {
                Box(
                    modifier = Modifier
                        .width(profileImageSize / 2)
                        .height(profileImageSize / 2),
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                    )
                    CircularProgressIndicator(
                        progress = text.length.toFloat() / maxComposeTextLength.toFloat(),
                    )
                }
                Spacer(modifier = Modifier.weight(1F))
                if (locationEnabled) {
                    location?.let {
                        ProvideEmphasis(emphasis = AmbientEmphasisLevels.current.medium) {
                            Row {
                                Icon(asset = Icons.Default.Place)
                                Text(text = "${it.latitude}, ${it.longitude}")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(standardPadding * 2))
            Divider()
            ComposeActions()
        }
    }
}

@Composable
private fun ComposeActions() {
    val viewModel = navViewModel<ComposeViewModel>()
    val launcher = AmbientLauncher.current
    val scope = rememberCoroutineScope()
    Box {
        Row {
            IconButton(
                onClick = {
                    scope.launch {
                        val item = launcher.launchForResult(ActivityResultContracts.GetMultipleContents())
                    }
//                            openImagePicker()
                }
            ) {
                Icon(asset = Icons.Default.Camera)
            }
            IconButton(onClick = {}) {
                Icon(asset = Icons.Default.Gif)
            }
            IconButton(onClick = {}) {
                Icon(asset = Icons.Default.AlternateEmail)
            }
            IconButton(onClick = {}) {
                Icon(asset = Icons.Default.Topic)
            }
            IconButton(
                onClick = {
//                            if (locationEnabled) {
//                                disableLocation()
//                            } else {
//                                getOrRequestLocation()
//                            }
                },
            ) {
                Icon(asset = Icons.Default.MyLocation)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(asset = Icons.Default.Pages)
            }
        }
    }
}


@Composable
private fun ComposeImage(item: Uri) {
    val viewModel = navViewModel<ComposeViewModel>()
    var expanded by remember { mutableStateOf(false) }
    val image = @Composable {
        Box(
            modifier = Modifier
                .heightIn(max = composeImageSize)
                .aspectRatio(1F)
                .clickable(
                    onClick = {
                        expanded = true
                    }
                )
                .clip(MaterialTheme.shapes.small),
        ) {
            NetworkImage(url = item)
        }
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        toggle = image,
    ) {
        DropdownMenuItem(
            onClick = {
                expanded = false
                viewModel.removeImage(item)
            }
        ) {
            Text("Remove")
        }
    }
}