package com.thetmyopaing.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thetmyopaing.ledger.data.AgentEntity
import com.thetmyopaing.ledger.data.CustomerEntity
import com.thetmyopaing.ledger.data.LedgerSnapshot
import com.thetmyopaing.ledger.domain.QuickFormat
import com.thetmyopaing.ledger.domain.allDigits
import com.thetmyopaing.ledger.domain.commission
import com.thetmyopaing.ledger.domain.payout
import java.time.LocalDate

private sealed interface Route {
    data object Agents : Route
    data class AgentHome(val id: String) : Route
    data class Customers(val agentId: String) : Route
    data class AgentTotalList(val agentId: String) : Route
    data class CustomerHome(val id: String) : Route
    data class Betting(val customerId: String) : Route
    data class CustomerList(val customerId: String) : Route
    data class CustomerReport(val customerId: String) : Route
    data class AgentReport(val agentId: String) : Route
    data object GlobalNumbers : Route
    data object ClosedDays : Route
    data class ClosedNumbers(val agentId: String) : Route
    data object Format : Route
    data class CustomerSettings(val customerId: String) : Route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp(viewModel: LedgerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var route by remember { mutableStateOf<Route>(Route.Agents) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.busy && uiState.snapshot.agents.isEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            when (val current = route) {
                Route.Agents -> AgentListScreen(
                    snapshot = uiState.snapshot,
                    onAgent = { route = Route.AgentHome(it) },
                    onGlobalNumbers = { route = Route.GlobalNumbers },
                    onClosedDays = { route = Route.ClosedDays },
                    viewModel = viewModel,
                )
                is Route.AgentHome -> AgentHomeScreen(
                    agent = uiState.snapshot.agents.firstOrNull { it.id == current.id },
                    snapshot = uiState.snapshot,
                    onBack = { route = Route.Agents },
                    onCustomers = { route = Route.Customers(current.id) },
                    onTotalList = { route = Route.AgentTotalList(current.id) },
                    onClosedNumbers = { route = Route.ClosedNumbers(current.id) },
                    onFormat = { route = Route.Format },
                    onReport = { route = Route.AgentReport(current.id) },
                    onNumbers = { route = Route.GlobalNumbers },
                )
                is Route.Customers -> CustomerListScreen(
                    agentId = current.agentId,
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.AgentHome(current.agentId) },
                    onCustomer = { route = Route.CustomerHome(it) },
                )
                is Route.AgentTotalList -> AgentTotalListScreen(
                    agentId = current.agentId,
                    snapshot = uiState.snapshot,
                    onBack = { route = Route.AgentHome(current.agentId) },
                )
                is Route.CustomerHome -> CustomerHomeScreen(
                    customer = uiState.snapshot.customers.firstOrNull { it.id == current.id },
                    snapshot = uiState.snapshot,
                    onBack = {
                        val agentId = uiState.snapshot.customers.firstOrNull { it.id == current.id }?.agentId
                        if (agentId != null) route = Route.Customers(agentId) else route = Route.Agents
                    },
                    onBet = { route = Route.Betting(current.id) },
                    onList = { route = Route.CustomerList(current.id) },
                    onReport = { route = Route.CustomerReport(current.id) },
                    onSettings = { route = Route.CustomerSettings(current.id) },
                    onNumbers = { route = Route.GlobalNumbers },
                )
                is Route.Betting -> BettingScreen(
                    customer = uiState.snapshot.customers.firstOrNull { it.id == current.customerId },
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.CustomerHome(current.customerId) },
                )
                is Route.CustomerList -> CustomerNumberGridScreen(
                    customer = uiState.snapshot.customers.firstOrNull { it.id == current.customerId },
                    snapshot = uiState.snapshot,
                    onBack = { route = Route.CustomerHome(current.customerId) },
                )
                is Route.CustomerReport -> CustomerReportScreen(
                    customer = uiState.snapshot.customers.firstOrNull { it.id == current.customerId },
                    snapshot = uiState.snapshot,
                    onBack = { route = Route.CustomerHome(current.customerId) },
                )
                is Route.AgentReport -> AgentReportScreen(
                    agentId = current.agentId,
                    snapshot = uiState.snapshot,
                    onBack = { route = Route.AgentHome(current.agentId) },
                )
                Route.GlobalNumbers -> GlobalNumbersScreen(
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.Agents },
                )
                Route.ClosedDays -> ClosedDaysScreen(
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.Agents },
                )
                is Route.ClosedNumbers -> ClosedNumbersScreen(
                    agentId = current.agentId,
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.AgentHome(current.agentId) },
                )
                Route.Format -> FormatScreen(onBack = { route = Route.Agents })
                is Route.CustomerSettings -> CustomerSettingsScreen(
                    customer = uiState.snapshot.customers.firstOrNull { it.id == current.customerId },
                    snapshot = uiState.snapshot,
                    viewModel = viewModel,
                    onBack = { route = Route.CustomerHome(current.customerId) },
                )
            }
        }
    }
}

private fun money(value: Long): String = "%,d".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    }
                },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun AgentListScreen(
    snapshot: LedgerSnapshot,
    onAgent: (String) -> Unit,
    onGlobalNumbers: () -> Unit,
    onClosedDays: () -> Unit,
    viewModel: LedgerViewModel,
) {
    var showAdd by remember { mutableStateOf(false) }
    LedgerScaffold(
        title = "2D Ledger",
        subtitle = "Agent workspace • Offline",
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("နေ့စဉ်စာရင်းကို ရှင်းရှင်းလင်းလင်း ထိန်းချုပ်ပါ", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Agent၊ Customer နဲ့ 00–99 စာရင်းအားလုံးကို Offline သိမ်းထားနိုင်ပါတယ်")
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { showAdd = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Agent အသစ်ထည့်ရန်")
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        label = "Closed Day",
                        value = "${snapshot.closedDays.size} ရက်",
                        onClick = onClosedDays,
                    )
                    UtilityTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Numbers,
                        label = "ထီပေါက်စဉ်",
                        value = "${snapshot.winningNumbers.size} ခု",
                        onClick = onGlobalNumbers,
                    )
                }
            }
            item {
                Text("Agents", style = MaterialTheme.typography.titleLarge)
            }
            if (snapshot.agents.isEmpty()) {
                item {
                    EmptyCard(
                        title = "Agent မရှိသေးပါ",
                        body = "ပထမဆုံး Agent ကိုထည့်ပြီး စာရင်းစတင်ပါ",
                        action = "Agent ထည့်ရန်",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(snapshot.agents) { agent ->
                    AgentCard(
                        agent = agent,
                        customerCount = snapshot.customers.count { it.agentId == agent.id },
                        onClick = { onAgent(agent.id) },
                    )
                }
            }
        }
    }
    if (showAdd) {
        AgentFormDialog(
            onDismiss = { showAdd = false },
            onSave = { name, address, phone, rate, remark ->
                viewModel.saveAgent(null, name, address, phone, rate, remark)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AgentCard(agent: AgentEntity, customerCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(agent.name, style = MaterialTheme.typography.titleMedium)
                Text("$customerCount Customers • Rate ${agent.rate}", style = MaterialTheme.typography.bodySmall)
            }
            Text("ဝင်ရန်", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UtilityTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AgentHomeScreen(
    agent: AgentEntity?,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
    onCustomers: () -> Unit,
    onTotalList: () -> Unit,
    onClosedNumbers: () -> Unit,
    onFormat: () -> Unit,
    onReport: () -> Unit,
    onNumbers: () -> Unit,
) {
    if (agent == null) return
    val customerCount = snapshot.customers.count { it.agentId == agent.id }
    LedgerScaffold(title = agent.name, subtitle = "Rate ${agent.rate} • Agent workspace", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(
                    title = "Agent overview",
                    value = "$customerCount",
                    caption = "Customers • Rate ${agent.rate}",
                    icon = Icons.Default.WorkspacePremium,
                )
            }
            item { Text("Workspace", style = MaterialTheme.typography.titleLarge) }
            item {
                ActionGrid(
                    listOf(
                        Triple("Customer", "$customerCount ယောက်", Icons.Default.Groups) to onCustomers,
                        Triple("Total List", "00–99 စုစုပေါင်း", Icons.Default.GridView) to onTotalList,
                        Triple("Closed Number", "Agent သီးခြား", Icons.Default.Close) to onClosedNumbers,
                        Triple("Format", "Reference", Icons.Default.Info) to onFormat,
                        Triple("Report", "Customer-by-customer", Icons.Default.Assessment) to onReport,
                        Triple("ထီပေါက်စဉ်", "Global results", Icons.Default.Numbers) to onNumbers,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(items: List<Pair<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (item, action) ->
                    val (title, subtitle, icon) = item
                    Card(Modifier.weight(1f).clickable(onClick = action)) {
                        Column(Modifier.padding(15.dp)) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CustomerListScreen(
    agentId: String,
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val agent = snapshot.agents.firstOrNull { it.id == agentId }
    val customers = snapshot.customers.filter { it.agentId == agentId }
    LedgerScaffold(
        title = "Customer",
        subtitle = agent?.name ?: "",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FilledTonalButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Customer အသစ်ထည့်ရန်")
                }
            }
            if (customers.isEmpty()) {
                item {
                    EmptyCard(
                        title = "Customer မရှိသေးပါ",
                        body = "ဒီ Agent အောက်မှာ Customer ထည့်ပါ",
                        action = "Customer ထည့်ရန်",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(customers) { customer ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onCustomer(customer.id) },
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(customer.name, style = MaterialTheme.typography.titleMedium)
                                Text(customer.phone.ifBlank { "Customer profile" }, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("ဝင်ရန်", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        CustomerFormDialog(
            onDismiss = { showAdd = false },
            onSave = { name, address, phone, remark ->
                viewModel.saveCustomer(null, agentId, name, address, phone, remark)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AgentTotalListScreen(
    agentId: String,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
) {
    val customerIds = snapshot.customers.filter { it.agentId == agentId }.map { it.id }.toSet()
    val totals = snapshot.entries.filter { it.customerId in customerIds }
        .groupingBy { it.digit }
        .fold(0L) { total, entry -> total + entry.amount }
    LedgerScaffold("Total List", "Agent aggregate • 00–99", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(allDigits()) { digit ->
                NumberCard(digit, totals[digit] ?: 0L, false, false)
            }
        }
    }
}

@Composable
private fun CustomerHomeScreen(
    customer: CustomerEntity?,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
    onBet: () -> Unit,
    onList: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
    onNumbers: () -> Unit,
) {
    if (customer == null) return
    val total = snapshot.entries.filter { it.customerId == customer.id }.sumOf { it.amount }
    val slots = snapshot.entries.filter { it.customerId == customer.id }.map { it.digit }.toSet().size
    LedgerScaffold("Customer", customer.name, onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("စုစုပေါင်းထိုးကြေး", style = MaterialTheme.typography.labelMedium)
                            Text("${money(total)} ကျပ်", style = MaterialTheme.typography.headlineMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("အကွက်", style = MaterialTheme.typography.labelMedium)
                            Text("$slots / 100", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onBet,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Paid, null)
                    Spacer(Modifier.width(8.dp))
                    Text("စာရင်းသွင်းရန်", fontSize = 17.sp)
                }
            }
            item {
                ActionGrid(
                    listOf(
                        Triple("ထီပေါက်စဉ်", "Customer view", Icons.Default.Numbers) to onNumbers,
                        Triple("List", "00–99 grid", Icons.Default.GridView) to onList,
                        Triple("Report", "Daily / Weekly", Icons.Default.Assessment) to onReport,
                        Triple("Limit / Commission", "Settings", Icons.Default.Tune) to onSettings,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BettingScreen(
    customer: CustomerEntity?,
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
) {
    if (customer == null) return
    val agent = snapshot.agents.firstOrNull { it.id == customer.agentId }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var session by remember { mutableStateOf("မနက်") }
    var rawInput by remember { mutableStateOf("") }
    var quickAmount by remember { mutableStateOf("100") }
    val preview = viewModel.preview(customer.id, customer.agentId, rawInput)

    LedgerScaffold("စာရင်းသွင်းရန်", customer.name, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("ရက်စွဲ") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                SessionChoice(session, onChange = { session = it })
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Quick selection", style = MaterialTheme.typography.titleMedium)
                    Text("ပမာဏ", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = quickAmount,
                        onValueChange = { quickAmount = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        QuickFormat.values().take(3).forEach { format ->
                            AssistChip(
                                onClick = {
                                    rawInput = format.description.replace(" ", ".") + ".$quickAmount"
                                },
                                label = { Text(format.title) },
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        QuickFormat.values().drop(3).forEach { format ->
                            AssistChip(
                                onClick = {
                                    rawInput = format.description.replace(" ", ".") + ".$quickAmount"
                                },
                                label = { Text(format.title) },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Input Box") },
                placeholder = { Text("10.100  သို့မဟုတ်  10R100") },
                supportingText = { Text("Paste၊ manual input နဲ့ R / Reverse ကိုလည်း လက်ခံပါတယ်") },
            )
            Text("Preview / Status", style = MaterialTheme.typography.titleMedium)
            if (preview.isEmpty()) {
                Card {
                    Text(
                        "Input ထည့်လိုက်ရင် ဂဏန်းတစ်ခုချင်းစီရဲ့ လက်ရှိပမာဏ၊ အသစ်ထည့်မယ့်ပမာဏနဲ့ Limit ကို ပြပါမယ်",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                preview.forEach { row ->
                    PreviewCard(row.digit, row.current, row.input, row.after, row.limit, row.closed)
                }
            }
            Button(
                onClick = {
                    if (agent != null) viewModel.saveBet(customer.id, agent.id, date, session, rawInput)
                },
                enabled = preview.isNotEmpty() && preview.all { it.allowed },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Confirm & Save", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SessionChoice(value: String, onChange: (String) -> Unit) {
    Column {
        Text("Draw", style = MaterialTheme.typography.labelMedium)
        Row {
            FilterPill("မနက်", value == "မနက်") { onChange("မနက်") }
            FilterPill("ညနေ", value == "ညနေ") { onChange("ညနေ") }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 5.dp),
        leadingIcon = if (selected) ({ Icon(Icons.Default.Paid, null, Modifier.size(14.dp)) }) else null,
    )
}

@Composable
private fun PreviewCard(
    digit: String,
    current: Long,
    input: Long,
    after: Long,
    limit: Long?,
    closed: Boolean,
) {
    val good = !closed && (limit == null || after <= limit)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                closed -> MaterialTheme.colorScheme.errorContainer
                good -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(digit, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text("လက်ရှိ ${money(current)} + အသစ် ${money(input)}", style = MaterialTheme.typography.bodySmall)
                Text("ပြီးနောက် ${money(after)}", fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(limit?.let { "Limit ${money(it)}" } ?: "No limit", style = MaterialTheme.typography.labelSmall)
                Text(
                    when {
                        closed -> "Closed"
                        good -> "Allowed"
                        else -> "Over limit"
                    },
                    color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CustomerNumberGridScreen(
    customer: CustomerEntity?,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
) {
    if (customer == null) return
    val totals = snapshot.entries.filter { it.customerId == customer.id }
        .groupingBy { it.digit }
        .fold(0L) { total, entry -> total + entry.amount }
    val settings = snapshot.settings.firstOrNull { it.customerId == customer.id }
    val specials = snapshot.specialLimits.filter { it.customerId == customer.id }.associate { it.digit to it.amount }
    val closed = snapshot.closedNumbers.filter {
        it.agentId == customer.agentId
    }.map { it.digit }.toSet()
    LedgerScaffold("List", customer.name, onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(allDigits()) { digit ->
                val limit = specials[digit] ?: settings?.allLimit
                NumberCard(digit, totals[digit] ?: 0L, digit in closed, limit != null, limit)
            }
        }
    }
}

@Composable
private fun NumberCard(
    digit: String,
    total: Long,
    closed: Boolean,
    special: Boolean,
    limit: Long? = null,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                closed -> MaterialTheme.colorScheme.errorContainer
                special -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(digit, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(money(total), style = MaterialTheme.typography.labelSmall)
            limit?.let { Text("/ ${money(it)}", style = MaterialTheme.typography.labelSmall) }
            if (closed) Text("ပိတ်", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CustomerReportScreen(
    customer: CustomerEntity?,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
) {
    if (customer == null) return
    var after by remember { mutableStateOf(false) }
    val agent = snapshot.agents.firstOrNull { it.id == customer.agentId }
    val entriesByRecord = snapshot.entries.groupBy { it.recordId }
    val records = snapshot.records.filter { it.customerId == customer.id }
    LedgerScaffold("Report", customer.name, onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Before", !after) { after = false }
                FilterPill("After", after) { after = true }
            }
            Spacer(Modifier.height(12.dp))
            if (records.isEmpty()) {
                EmptyCard("Report မရှိသေးပါ", "စာရင်းသွင်းပြီးနောက် report ပြပါမယ်", null, {})
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records) { record ->
                        val bets = entriesByRecord[record.id].orEmpty()
                        val total = bets.sumOf { it.amount }
                        val winning = if (after) {
                            val result = snapshot.winningNumbers.firstOrNull {
                                it.date == record.drawDate && it.session == record.session
                            }
                            result?.let { resultNumber -> bets.filter { it.digit == resultNumber.digit }.sumOf { it.amount } } ?: 0
                        } else 0
                        val payoutValue = agent?.let { payout(winning.toLong(), it.rate) } ?: 0
                        val customerSettings = snapshot.settings.firstOrNull { it.customerId == customer.id }
                        Card {
                            Column(Modifier.padding(15.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${record.drawDate} • ${record.session}", fontWeight = FontWeight.Bold)
                                    Text("${bets.size} အကွက်", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(7.dp))
                                Text("ထိုးကြေး ${money(total)} ကျပ်")
                                Text("Commission ${money(commission(total, customerSettings?.commissionRate ?: 0))} ကျပ်")
                                if (after) {
                                    Text("ပေါက်ကြေး ${money(winning.toLong())} ကျပ်")
                                    Text("လျော်ပေးငွေ ${money(payoutValue)} ကျပ်")
                                    Text(
                                        "အရှုံး/အမြတ် ${money(total - payoutValue)} ကျပ်",
                                        fontWeight = FontWeight.Bold,
                                        color = if (total - payoutValue >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                } else {
                                    TextButton(onClick = {}) { Text("Before report") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentReportScreen(
    agentId: String,
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
) {
    val customerIds = snapshot.customers.filter { it.agentId == agentId }.map { it.id }.toSet()
    val agent = snapshot.agents.firstOrNull { it.id == agentId }
    val entriesByRecord = snapshot.entries.groupBy { it.recordId }
    val records = snapshot.records.filter { it.customerId in customerIds }
    val totals = records.map { record ->
        val bets = entriesByRecord[record.id].orEmpty()
        val total = bets.sumOf { it.amount }
        val result = snapshot.winningNumbers.firstOrNull { it.date == record.drawDate && it.session == record.session }
        val winning = result?.let { r -> bets.filter { it.digit == r.digit }.sumOf { it.amount } } ?: 0
        Triple(total, winning, agent?.let { payout(winning, it.rate) } ?: 0)
    }
    LedgerScaffold("Agent Report", agent?.name ?: "", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SummaryCard(
                    "စုစုပေါင်းထိုးကြေး",
                    "${money(totals.sumOf { it.first })} ကျပ်",
                    "Customer-by-customer source records",
                    Icons.Default.Assessment,
                )
            }
            items(records) { record ->
                val bets = entriesByRecord[record.id].orEmpty()
                val total = bets.sumOf { it.amount }
                val customer = snapshot.customers.firstOrNull { it.id == record.customerId }
                val winning = snapshot.winningNumbers.firstOrNull {
                    it.date == record.drawDate && it.session == record.session
                }?.let { result -> bets.filter { it.digit == result.digit }.sumOf { it.amount } } ?: 0
                val payoutValue = agent?.let { payout(winning, it.rate) } ?: 0
                Card {
                    Column(Modifier.padding(15.dp)) {
                        Text("${record.drawDate} • ${record.session}", fontWeight = FontWeight.Bold)
                        Text(customer?.name ?: "Customer")
                        Text("ထိုးကြေး ${money(total)} • ပေါက်ကြေး ${money(winning)}")
                        Text("လျော်ပေးငွေ ${money(payoutValue)} • ရှုံး/မြတ် ${money(total - payoutValue)}")
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text("ထိုးကြေး ${money(totals.sumOf { it.first })}")
                        Text("ပေါက်ကြေး ${money(totals.sumOf { it.second })}")
                        Text("လျော်ပေးငွေ ${money(totals.sumOf { it.third })}")
                        Text("ရှုံး/မြတ် ${money(totals.sumOf { it.first } - totals.sumOf { it.third })}")
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalNumbersScreen(
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var session by remember { mutableStateOf("မနက်") }
    var digit by remember { mutableStateOf("") }
    LedgerScaffold("ထီပေါက်စဉ်", "Global winning numbers", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ပေါက်ဂဏန်းထည့်ရန်", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(date, { date = it }, label = { Text("ရက်စွဲ") }, modifier = Modifier.fillMaxWidth())
                        SessionChoice(session) { session = it }
                        OutlinedTextField(
                            digit,
                            { digit = it },
                            label = { Text("Digit (00–99)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Button(onClick = { viewModel.saveWinningNumber(date, session, digit) }) {
                            Text("Save winning number")
                        }
                    }
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleLarge) }
            items(snapshot.winningNumbers) { result ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${result.date} • ${result.session}", fontWeight = FontWeight.Bold)
                            Text("ထွက်ဂဏန်း", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(result.digit, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { viewModel.deleteWinningNumber(result.id) }) {
                            Icon(Icons.Default.Close, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedDaysScreen(
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    LedgerScaffold("Closed Day", "Global • all Agents and Customers", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Business ပိတ်မယ့်ရက်", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(date, { date = it }, label = { Text("ရက်စွဲ (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { viewModel.saveClosedDay(date) }) { Text("Closed Day ထည့်ရန်") }
                    }
                }
            }
            items(snapshot.closedDays) { day ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(day.date, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteClosedDay(day.id) }) {
                            Icon(Icons.Default.Close, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedNumbersScreen(
    agentId: String,
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
) {
    var digit by remember { mutableStateOf("") }
    val agent = snapshot.agents.firstOrNull { it.id == agentId }
    LedgerScaffold("Closed Number", agent?.name ?: "", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ဒီ Agent အောက်မှာ ပိတ်မယ့်ဂဏန်း", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            digit,
                            { digit = it },
                            label = { Text("Digit (00–99)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Button(onClick = { viewModel.saveClosedNumber(agentId, digit) }) { Text("Save") }
                    }
                }
            }
            items(snapshot.closedNumbers.filter { it.agentId == agentId }) { closed ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(closed.digit, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Closed", color = MaterialTheme.colorScheme.error)
                        IconButton(onClick = { viewModel.deleteClosedNumber(closed.id) }) {
                            Icon(Icons.Default.Close, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatScreen(onBack: () -> Unit) {
    val rows = listOf(
        "ပါဝါ" to "05, 50, 16, 61, 27, 72, 38, 83, 49, 94 • 10 entries",
        "နက္ခတ်" to "07, 70, 18, 81, 24, 42, 35, 53, 69, 96 • 10 entries",
        "အပူး" to "00, 11, 22, 33, 44, 55, 66, 77, 88, 99 • 10 entries",
        "ညီအကို" to "01/10, 12/21, 23/32 ... 09/90 • 20 entries",
        "အခွေ" to "Source digits အချင်းချင်း နှစ်ဖက်လှည့် • 345 = 6 entries",
        "အခွေပူး" to "အခွေ combinations + source digit တစ်ခုချင်းစီ double",
        "ပတ်သီး" to "9 → 09, 90, 19, 91 ... 99 • 19 unique entries",
        "ထိပ်စည်း" to "9 → 90–99 • 10 entries",
        "နောက်ပိတ်" to "9 → 09, 19 ... 99 • 10 entries",
        "R / Reverse" to "10R100 → 10 = 100, 01 = 100",
    )
    LedgerScaffold("Format", "Reference & quick-selection guide", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "Format စာမျက်နှာသည် reference ဖြစ်ပြီး calculation engine မဟုတ်ပါ။ Input အားလုံးသည် parser တစ်ခုတည်းကို ဖြတ်သွားပါမယ်။",
                        Modifier.padding(16.dp),
                    )
                }
            }
            items(rows) { (title, description) ->
                Card {
                    Column(Modifier.padding(15.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(5.dp))
                        Text(description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSettingsScreen(
    customer: CustomerEntity?,
    snapshot: LedgerSnapshot,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
) {
    if (customer == null) return
    val current = snapshot.settings.firstOrNull { it.customerId == customer.id }
    var commissionRate by remember(current?.commissionRate) { mutableStateOf((current?.commissionRate ?: 0).toString()) }
    var allLimit by remember(current?.allLimit) { mutableStateOf(current?.allLimit?.toString() ?: "") }
    var specialDigit by remember { mutableStateOf("") }
    var specialAmount by remember { mutableStateOf("") }
    val specials = snapshot.specialLimits.filter { it.customerId == customer.id }

    LedgerScaffold("Limit & Commission", customer.name, onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Customer settings", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            commissionRate,
                            { commissionRate = it },
                            label = { Text("Commission Rate (%)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            allLimit,
                            { allLimit = it },
                            label = { Text("All Limit (blank = no limit)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Button(
                            onClick = { viewModel.saveSettings(customer.id, commissionRate, allLimit) },
                        ) { Text("Save settings") }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Special Limit", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                specialDigit,
                                { specialDigit = it },
                                label = { Text("Digit") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            OutlinedTextField(
                                specialAmount,
                                { specialAmount = it },
                                label = { Text("Amount") },
                                modifier = Modifier.weight(1.4f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveSpecialLimit(customer.id, specialDigit, specialAmount)
                                specialDigit = ""
                                specialAmount = ""
                            },
                        ) { Text("Add special limit") }
                    }
                }
            }
            items(specials) { limit ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(limit.digit, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp))
                        Text("Limit ${money(limit.amount)}", Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteSpecialLimit(limit.id) }) {
                            Icon(Icons.Default.Close, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(caption, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
            action?.let {
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onAction) { Text(it) }
            }
        }
    }
}

@Composable
private fun AgentFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("80") }
    var remark by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agent အသစ်") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    rate,
                    { rate = it },
                    label = { Text("Rate") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(remark, { remark = it }, label = { Text("Remark") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, address, phone, rate, remark) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CustomerFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customer အသစ်") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("အမည်") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("လိပ်စာ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("ဖုန်း") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(remark, { remark = it }, label = { Text("မှတ်ချက်") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, address, phone, remark) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}