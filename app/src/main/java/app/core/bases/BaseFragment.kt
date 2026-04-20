package app.core.bases

import android.content.*
import android.os.*
import android.view.*
import androidx.fragment.app.*
import java.lang.ref.*

abstract class BaseFragment : Fragment() {

	private var weakActivityRef: WeakReference<BaseActivity>? = null
	private var weakFragmentRef: WeakReference<BaseFragment>? = null
	private var _fragmentLayout: View? = null

	open val safeActivityRef: BaseActivity? get() = weakActivityRef?.get()
	open val safeFragmentRef: BaseFragment? get() = weakFragmentRef?.get()
	open val safeFragmentLayoutRef: View? get() = _fragmentLayout

	open var isFragmentRunning: Boolean = false

	protected abstract fun getLayoutResId(): Int
	protected abstract fun onAfterLayoutLoad(layoutView: View, state: Bundle?)
	protected abstract fun onResumeFragment()
	protected abstract fun onPauseFragment()

	override fun onAttach(context: Context) {
		super.onAttach(context)
		weakActivityRef = WeakReference(context as BaseActivity)
		weakFragmentRef = WeakReference(this)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		return inflater.inflate(getLayoutResId(), container, false)
			.also { _fragmentLayout = it }
	}

	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		onAfterLayoutLoad(view, bundle)
	}

	override fun onResume() {
		super.onResume()
		isFragmentRunning = true
		onResumeFragment()
	}

	override fun onPause() {
		super.onPause()
		isFragmentRunning = false
		onPauseFragment()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		isFragmentRunning = false
		_fragmentLayout = null
		weakActivityRef?.clear()
		weakActivityRef = null

		weakFragmentRef?.clear()
		weakFragmentRef = null
	}
}
