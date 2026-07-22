package com.q25.bbos7

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.q25.bbos7.data.AppClassifier
import com.q25.bbos7.data.AppEntry
import com.q25.bbos7.data.AppRepository
import com.q25.bbos7.data.AppTab
import com.q25.bbos7.data.PinnedApps
import com.q25.bbos7.data.Shortcuts
import com.q25.bbos7.data.TabOverrides
import com.q25.bbos7.ui.AppGridAdapter
import com.q25.bbos7.ui.StatusPanelView
import com.q25.bbos7.ui.TabCarouselView
import com.q25.bbos7.ui.TabPagerAdapter
import com.q25.bbos7.ui.TrayLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/**
 * Man hinh chinh chinh la panel keo len — giong BBOS7, khong co luoi icon rieng
 * o lop nen. Thanh tab luon hien, ke ca khi panel chi con mot dong icon; vuot
 * ngang tren luoi doi tab va thanh tab truot theo cung nhip.
 */
class LauncherActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private lateinit var pinned: PinnedApps
    private lateinit var classifier: AppClassifier
    private lateinit var overrides: TabOverrides
    private lateinit var shortcuts: Shortcuts

    private lateinit var tray: TrayLayout
    private lateinit var statusPanel: StatusPanelView
    private lateinit var tabCarousel: TabCarouselView
    private lateinit var tabPager: ViewPager2
    private lateinit var pagerAdapter: TabPagerAdapter
    private lateinit var appLabel: TextView
    private lateinit var searchField: EditText
    private lateinit var emptyMessage: TextView

    private var allApps: List<AppEntry> = emptyList()
    private var currentTab: AppTab = AppTab.FAVORITES

    /** null nghia la khong o che do tim kiem. */
    private var searchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        pinned = PinnedApps(this)
        classifier = AppClassifier(this)
        overrides = TabOverrides(this)
        shortcuts = Shortcuts(this)
        repository = AppRepository(this, lifecycleScope)

        appLabel = findViewById(R.id.app_label)
        emptyMessage = findViewById(R.id.empty_message)
        searchField = findViewById(R.id.search_field)
        tabCarousel = findViewById(R.id.tab_carousel)
        tabPager = findViewById(R.id.tab_pager)

        setUpPager()
        setUpTray()
        setUpSearchField()

        statusPanel = findViewById<StatusPanelView>(R.id.status_panel).apply {
            onSearchClick = { startSearch(initial = null) }
        }
        drawBehindSystemStatusBar()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    searchQuery != null -> stopSearch()
                    else -> tray.collapseIfOpen()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.apps.collectLatest { apps ->
                    allApps = apps
                    if (apps.isNotEmpty()) pinned.seedIfEmpty(this@LauncherActivity, apps)
                    refresh()
                }
            }
        }

        repository.start()
    }

    override fun onDestroy() {
        repository.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        statusPanel.refresh()
    }

    /** Bam Home khi da o launcher: ve dung trang thai man hinh chinh. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (searchQuery != null) stopSearch()
        tray.collapseIfOpen()
    }

    /**
     * Khi chua co gi duoc chon, cu di trackpad theo huong nao cung phai vao
     * icon dau tien — de Android tu tim thi no hay nhay vao mot nut linh tinh
     * o vung dinh.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && isDirectionKey(event.keyCode)) {
            val focused = currentFocus
            if (focused == null || focused === window.decorView) {
                val first = currentGrid()?.getChildAt(0)
                if (first?.requestFocus() == true) return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && scrollToNextRow()) return true
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP && backToSearchField()) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Duong ve: dang o hang icon dau tien va an len thi quay lai o tim kiem. Cung ly do nhu luc
     * di xuong — hai lop RecyclerView long nhau chan mat duong focus tu nhien.
     */
    private fun backToSearchField(): Boolean {
        if (searchQuery == null) return false
        val grid = currentGrid() ?: return false
        val focused = grid.focusedChild ?: return false
        val position = grid.getChildAdapterPosition(focused)
        if (position == RecyclerView.NO_POSITION || position >= COLUMNS) return false
        return searchField.requestFocus()
    }

    /**
     * Dang o hang duoi cung nhin thay va an xuong tiep: hang ke tiep nam ngoai man hinh nen dang
     * bi chan focus, va Android thi khong tu cuon toi mot view khong nhan focus duoc. Tu cuon len
     * mot hang roi tha focus vao do — neu khong, trackpad se dung khung o hang cuoi, khong cach
     * nao xuong tiep tru khi lay tay vuot.
     */
    private fun scrollToNextRow(): Boolean {
        val grid = currentGrid() ?: return false
        val gridAdapter = grid.adapter as? AppGridAdapter ?: return false
        val focused = grid.focusedChild ?: return false

        val position = grid.getChildAdapterPosition(focused)
        if (position == RecyclerView.NO_POSITION) return false

        val next = position + COLUMNS
        // Con cho di xuong binh thuong, hoac da het danh sach: de Android tu xu ly.
        if (next >= gridAdapter.itemCount || next < gridAdapter.focusableLimit) return false

        grid.scrollBy(0, focused.height)
        grid.post {
            updateFocusableLimit()
            // Doi them mot nhip: focusableLimit cung duoc dat trong mot lan post, va o moi phai
            // duoc phep nhan focus truoc khi doi focus sang no.
            grid.post { grid.findViewHolderForAdapterPosition(next)?.itemView?.requestFocus() }
        }
        return true
    }

    private fun isDirectionKey(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    /**
     * Go chu tren ban phim vat ly la mo tim kiem luon — dung kieu BlackBerry.
     * Chi bat khi chua o che do tim kiem; luc do EditText tu nhan phim.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isAltPressed && !event.isCtrlPressed && openShortcut(keyCode)) return true

        if (!event.isCtrlPressed && !event.isAltPressed) {
            val ch = event.unicodeChar.toChar()
            if (ch.isLetterOrDigit()) {
                when {
                    searchQuery == null -> startSearch(initial = ch.toString())
                    // Da o che do tim nhung o tim lo mat focus (vi du luoi icon
                    // vua gianh lay): tu keo focus ve va noi ky tu, dung de mat.
                    !searchField.hasFocus() -> appendToSearch(ch)
                    else -> return super.onKeyDown(keyCode, event)
                }
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DEL && searchQuery != null && !searchField.hasFocus()) {
                searchField.requestFocus()
                val text = searchField.text
                if (text.isNotEmpty()) text.delete(text.length - 1, text.length)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Alt + chu cai mo app da gan. Khong nhan khi dang tim kiem: luc do Alt la de go so — tren
     * Q25 khong co hang phim so rieng, Alt+D chinh la so 5.
     */
    private fun openShortcut(keyCode: Int): Boolean {
        if (searchQuery != null || !Shortcuts.isLetterKey(keyCode)) return false
        val componentKey = shortcuts.componentKeyOf(keyCode) ?: return false
        val entry = allApps.firstOrNull { it.key == componentKey } ?: return false
        launch(entry, tabPager)
        return true
    }

    private fun appendToSearch(ch: Char) {
        searchField.requestFocus()
        searchField.append(ch.toString())
        searchField.setSelection(searchField.text.length)
    }

    // --- Thiet lap ---------------------------------------------------------

    /**
     * Status bar cua Android hien thuong truc, nhung layout van tran len tan
     * mep tren de lop phu mo cua vung dinh chay lien qua ca vung status bar —
     * neu de he thong tu chua cho, cho do co nen rieng va lo mot vet ranh gioi.
     *
     * Da thu an han status bar: khong duoc. Khi vuot tu mep tren, Android goi
     * ra mot thanh "tam thoi" la lop phu va no khong bao thay doi insets, nen
     * vung dinh khong the biet ma tranh. Hien thuong truc thi inset on dinh.
     */
    private fun drawBehindSystemStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val base = resources.getDimensionPixelSize(R.dimen.status_panel_height)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusPanel.updateLayoutParams { height = base + top }
            statusPanel.updatePadding(top = top)
            tray.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = base + top }
            insets
        }
    }

    private fun setUpPager() {
        pagerAdapter = TabPagerAdapter(
            columns = COLUMNS,
            itemsFor = ::itemsForTab,
            onClick = ::launch,
            onLongClick = ::showMenu,
            onHighlight = ::showAppLabel,
            onScrolled = ::updateFocusableLimit,
        )
        tabPager.adapter = pagerAdapter
        tabPager.offscreenPageLimit = 1
        tabPager.setCurrentItem(TabCarouselView.centerPositionFor(currentTab), false)

        tabPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
                tabCarousel.syncTo(position, offset)
            }

            override fun onPageSelected(position: Int) {
                currentTab = AppTab.entries[position.mod(AppTab.entries.size)]
                showAppLabel(null)
                updateEmptyMessage()
                updatePageFocusability()
                updateFocusableLimit()
            }
        })

        // ViewPager2 chay bang mot RecyclerView noi bo, va cai do cung focusable
        // nen no nuot focus lam ca khoi luoi sang len thay vi mot icon. Khong
        // tat focus cua no duoc — tat di thi focus khong con duong nao xuong
        // luoi — nen cu de no nhan roi day tiep xuong o icon dau.
        (tabPager.getChildAt(0) as? RecyclerView)?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) currentGrid()?.getChildAt(0)?.requestFocus()
        }

        // Trang moi duoc gan vao cung phai bi khoa focus ngay, neu khong
        // trackpad se di lac sang luoi cua tab ben canh.
        (tabPager.getChildAt(0) as? RecyclerView)
            ?.addOnChildAttachStateChangeListener(
                object : RecyclerView.OnChildAttachStateChangeListener {
                    override fun onChildViewAttachedToWindow(view: View) =
                        updatePageFocusability()

                    override fun onChildViewDetachedFromWindow(view: View) = Unit
                }
            )

        // Cham vao tab dang o giua nghia la gap/mo panel; cham tab ben canh
        // thi truot toi tab do.
        tabCarousel.onTabClicked = { position ->
            if (position == tabPager.currentItem) {
                tray.toggle()
            } else {
                tabPager.setCurrentItem(position, true)
            }
        }
        tabCarousel.onToggleRequested = { tray.toggle() }
        tabCarousel.onTabStep = { step ->
            tabPager.setCurrentItem(tabPager.currentItem + step, true)
        }
        tabCarousel.onFocusDown = { currentGrid()?.getChildAt(0)?.requestFocus() == true }
    }

    private fun setUpTray() {
        tray = findViewById<TrayLayout>(R.id.tray).apply {
            scrollableChild = { currentGrid() }
            onStateSettled = { state ->
                if (state == TrayLayout.State.PEEK) {
                    // Ve man hinh chinh: quay lai tab Yeu thich tu dau danh sach.
                    if (searchQuery != null) stopSearch()
                    if (currentTab != AppTab.FAVORITES) goToTab(AppTab.FAVORITES)
                    currentGrid()?.scrollToPosition(0)
                    showAppLabel(null)
                } else if (searchQuery != null) {
                    // Dang tim kiem: o tim phai giu focus, neu khong ky tu tiep
                    // theo se roi vao luoi icon va mat hut.
                    searchField.requestFocus()
                } else {
                    // Cho trackpad mot diem bat dau. O che do cham tay thi
                    // requestFocus khong an gi ca, nen khong hai.
                    currentGrid()?.let { grid -> grid.post { grid.getChildAt(0)?.requestFocus() } }
                }
                updateFocusableLimit()
            }
        }
    }

    private fun setUpSearchField() {
        // Y het truong hop thanh tab: Android khong tu tim duoc duong tu day xuong luoi vi phai
        // di qua hai lop RecyclerView long nhau, nen trackpad dung khung o o tim kiem va khong
        // duyet duoc ket qua vua loc ra. Phai tu day focus xuong o dau tien.
        searchField.setOnKeyListener { _, keyCode, event ->
            event.action == KeyEvent.ACTION_DOWN &&
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
                currentGrid()?.getChildAt(0)?.requestFocus() == true
        }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (searchQuery != null) {
                    searchQuery = s?.toString().orEmpty()
                    refresh()
                }
            }
        })
    }

    // --- Dieu huong tab ----------------------------------------------------

    /** Nhay toi [tab] o chu ky gan vi tri hien tai nhat, de khong giat. */
    private fun goToTab(tab: AppTab) {
        val size = AppTab.entries.size
        val current = tabPager.currentItem
        tabPager.setCurrentItem(current - current.mod(size) + tab.ordinal, false)
    }

    /** RecyclerView cua trang dang hien. Doi moi khi vuot sang tab khac. */
    private fun currentGrid(): RecyclerView? {
        val inner = tabPager.getChildAt(0) as? RecyclerView ?: return null
        return inner.findViewHolderForAdapterPosition(tabPager.currentItem)
            ?.itemView as? RecyclerView
    }

    /**
     * Panel khong co lai kich thuoc khi keo — no chi truot xuong, nen phan duoi luoi bi mep man
     * hinh cat mat. RecyclerView khong he biet dieu do (no van cao het co), nen phai tu chan
     * trackpad khong cho focus xuong nhung icon dang nam ngoai man hinh.
     *
     * Do thang vi tri that cua tung o chu khong tinh "so hang nhin thay" roi nhan voi so cot:
     * cong thuc kia cho ra mot moc co dinh theo index, nen sau khi cuon, nhung icon vua duoc keo
     * len giua man hinh van bi chan vinh vien.
     */
    private fun updateFocusableLimit() {
        val grid = currentGrid() ?: return
        grid.post {
            val gridAdapter = grid.adapter as? AppGridAdapter ?: return@post
            val screenHeight = resources.displayMetrics.heightPixels
            val location = IntArray(2)

            var limit = 0
            var firstVisible = Int.MAX_VALUE
            for (i in 0 until grid.childCount) {
                val child = grid.getChildAt(i)
                val position = grid.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION) continue
                if (position < firstVisible) firstVisible = position

                child.getLocationOnScreen(location)
                val shown = minOf(location[1] + child.height, screenHeight) - maxOf(location[1], 0)
                // Khong doi o phai nam TRON trong man hinh: o nac PEEK hang icon duy nhat tho ra
                // dung mot chut, doi tron thi ca hang bi chan va focus roi nguoc ve container.
                if (shown < child.height * VISIBLE_RATIO) continue
                if (position + 1 > limit) limit = position + 1
            }

            // Luoi an toan: luon phai co mot hang nhan duoc focus, neu khong trackpad tu tren
            // xuong khong co cho bam va ca khoi luoi sang len.
            if (limit == 0 && firstVisible != Int.MAX_VALUE) limit = firstVisible + COLUMNS
            gridAdapter.focusableLimit = limit
        }
    }

    /**
     * ViewPager2 giu ca trang hai ben trong bo nho, va focus traversal cua
     * Android di qua moi view dang gan vao cua so — nen trackpad se duyet lac
     * sang luoi cua tab khac. Chan focus o moi trang tru trang dang hien.
     */
    private fun updatePageFocusability() {
        val inner = tabPager.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until inner.childCount) {
            val page = inner.getChildAt(i) as? ViewGroup ?: continue
            val isCurrent = inner.getChildAdapterPosition(page) == tabPager.currentItem
            page.descendantFocusability = if (isCurrent) {
                ViewGroup.FOCUS_AFTER_DESCENDANTS
            } else {
                ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        }
    }

    /** BBOS7 chi hien ten cua icon dang duoc chon, o mot dong rieng tren luoi. */
    private fun showAppLabel(entry: AppEntry?) {
        appLabel.text = entry?.label.orEmpty()
        // INVISIBLE chu khong GONE: dong ten phai luon giu cho, neu khong moi
        // lan no hien ra la ca hang icon bi day xuong va cut mat o day.
        appLabel.visibility = if (entry == null) View.INVISIBLE else View.VISIBLE
    }

    // --- Tim kiem ----------------------------------------------------------

    private fun startSearch(initial: String?) {
        searchQuery = initial.orEmpty()
        searchField.visibility = View.VISIBLE
        searchField.setText(initial.orEmpty())
        searchField.setSelection(searchField.text.length)
        // requestFocus ngay sau khi doi visibility la vo ich: View chi nhan
        // focus khi da co kich thuoc thuc, ma luc nay no chua qua layout nao.
        searchField.post {
            searchField.requestFocus()
            searchField.setSelection(searchField.text.length)
        }
        tray.animateTo(TrayLayout.State.FULL)
        // Tim kiem chay tren toan bo app, khong theo tab, nen giau thanh tab di.
        tabCarousel.visibility = View.GONE
        refresh()
    }

    private fun stopSearch() {
        searchQuery = null
        searchField.setText("")
        searchField.visibility = View.GONE
        searchField.clearFocus()
        tabCarousel.visibility = View.VISIBLE
        refresh()
    }

    // --- Du lieu -----------------------------------------------------------

    private fun itemsForTab(tab: AppTab): List<AppEntry> {
        val query = searchQuery ?: return classifier.filter(
            tab, allApps, pinned, overrides, repository::launchCount
        )
        return search(query)
    }

    private fun refresh() {
        (tabPager.getChildAt(0) as? RecyclerView)?.let { pagerAdapter.refreshVisiblePages(it) }
        updateEmptyMessage()
        tabPager.post {
            updatePageFocusability()
            updateFocusableLimit()
        }
    }

    private fun updateEmptyMessage() {
        val empty = itemsForTab(currentTab).isEmpty()
        emptyMessage.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) emptyMessage.setText(emptyMessageRes())
    }

    private fun emptyMessageRes(): Int = when {
        searchQuery != null -> R.string.empty_search
        currentTab == AppTab.FAVORITES -> R.string.empty_favorites
        currentTab == AppTab.FREQUENT -> R.string.empty_frequent
        else -> R.string.empty_generic
    }

    /** Tim khong phan biet dau: go "ky vuong" van ra "Kỳ Vương". */
    private fun search(query: String): List<AppEntry> {
        if (query.isBlank()) return allApps
        val needle = fold(query)
        return allApps.filter { fold(it.label).contains(needle) }
    }

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale("vi", "VN"))

    // --- Hanh dong ---------------------------------------------------------

    private fun launch(entry: AppEntry, source: View) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val options = ActivityOptions.makeScaleUpAnimation(
            source, 0, 0, source.width, source.height
        )
        try {
            startActivity(intent, options.toBundle())
            repository.recordLaunch(entry)
        } catch (e: Exception) {
            // App vua bi go nhung danh sach chua kip cap nhat.
            Toast.makeText(this, getString(R.string.launch_failed, entry.label), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showMenu(entry: AppEntry, anchor: View) {
        PopupMenu(this, anchor).apply {
            if (pinned.isPinned(entry)) {
                menu.add(0, MENU_UNPIN, 0, R.string.unpin)
            } else {
                menu.add(0, MENU_PIN, 0, R.string.pin)
            }

            // Moi tab mot o danh dau: tick la thuoc tab do, bo tick la go ra. Phan loai tu dong
            // chi la gia tri khoi diem — nguoi dung sap lai duoc ca hai chieu.
            val submenu = menu.addSubMenu(0, MENU_TABS, 1, R.string.assign_tab)
            for ((index, tab) in AppTab.assignable.withIndex()) {
                val member = overrides.stateOf(entry, tab)
                    ?: classifier.belongsAutomatically(entry, tab)
                submenu.add(GROUP_TABS, index, index, tab.titleRes).apply {
                    isCheckable = true
                    isChecked = member
                }
            }
            submenu.setGroupCheckable(GROUP_TABS, true, false)

            val assigned = shortcuts.keyCodeOf(entry)
            if (assigned == null) {
                menu.add(0, MENU_SHORTCUT, 2, R.string.assign_shortcut)
            } else {
                menu.add(
                    0, MENU_SHORTCUT_CLEAR, 2,
                    getString(R.string.clear_shortcut, Shortcuts.label(assigned)),
                )
            }

            menu.add(0, MENU_INFO, 3, R.string.app_info)
            setOnMenuItemClickListener { item ->
                if (item.groupId == GROUP_TABS) {
                    toggleTab(entry, AppTab.assignable[item.itemId], !item.isChecked)
                    true
                } else {
                    onMenuItem(item.itemId, entry)
                }
            }
            show()
        }
    }

    /**
     * Cho nguoi dung bam thang phim muon gan, thay vi bat ho chon tu mot danh sach 26 chu cai.
     * Dialog tu bat phim qua [android.content.DialogInterface.OnKeyListener].
     */
    private fun askForShortcutKey(entry: AppEntry) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.assign_shortcut_title, entry.label))
            .setMessage(R.string.assign_shortcut_hint)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            when {
                event.action != KeyEvent.ACTION_DOWN -> false
                keyCode == KeyEvent.KEYCODE_BACK -> false
                Shortcuts.isLetterKey(keyCode) -> {
                    assignShortcut(entry, keyCode)
                    dialog.dismiss()
                    true
                }
                // Nuot moi phim con lai: bam nham phim so khong nen lam dialog bien mat.
                else -> true
            }
        }
        dialog.show()
    }

    private fun assignShortcut(entry: AppEntry, keyCode: Int) {
        // Bao truoc khi cuop phim cua app khac, neu khong nguoi dung se tuong phim cu van con.
        val previous = shortcuts.componentKeyOf(keyCode)
            ?.let { key -> allApps.firstOrNull { it.key == key } }
            ?.takeIf { it.key != entry.key }

        shortcuts.assign(keyCode, entry)

        val label = Shortcuts.label(keyCode)
        val message = if (previous == null) {
            getString(R.string.shortcut_assigned, label, entry.label)
        } else {
            getString(R.string.shortcut_reassigned, label, entry.label, previous.label)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun toggleTab(entry: AppEntry, tab: AppTab, member: Boolean) {
        overrides.set(entry, tab, member, classifier.belongsAutomatically(entry, tab))
        refresh()
        val message = getString(
            if (member) R.string.added_to_tab else R.string.removed_from_tab,
            getString(tab.titleRes),
        )
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onMenuItem(itemId: Int, entry: AppEntry): Boolean = when (itemId) {
        MENU_PIN -> {
            pinned.pin(entry)
            refresh()
            true
        }

        MENU_UNPIN -> {
            pinned.unpin(entry)
            refresh()
            true
        }

        MENU_SHORTCUT -> {
            askForShortcutKey(entry)
            true
        }

        MENU_SHORTCUT_CLEAR -> {
            shortcuts.clear(entry)
            true
        }

        MENU_INFO -> {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", entry.component.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }

        else -> false
    }

    private companion object {
        /** Man hinh 720x720 @232dpi = 496x496dp — 6 cot nhu BBOS7, o ~82dp. */
        const val COLUMNS = 6

        /** O phai hien ra tung nay moi duoc phep nhan focus. */
        const val VISIBLE_RATIO = 0.75f

        const val MENU_PIN = 1
        const val MENU_UNPIN = 2
        const val MENU_INFO = 3
        const val MENU_TABS = 4
        const val MENU_SHORTCUT = 5
        const val MENU_SHORTCUT_CLEAR = 6
        const val GROUP_TABS = 10

        val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    }
}
