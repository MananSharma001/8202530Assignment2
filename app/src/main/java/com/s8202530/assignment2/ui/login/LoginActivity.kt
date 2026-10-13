package com.s8202530.assignment2.ui.login

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.s8202530.assignment2.BuildConfig
import com.s8202530.assignment2.R
import com.s8202530.assignment2.databinding.ActivityLoginBinding
import com.s8202530.assignment2.ui.common.applySystemBarPadding
import com.s8202530.assignment2.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Screen 1: Login. Sends the student ID and first name to the API and,
 * on success, opens the Dashboard with the keypass it returns.
 * @AndroidEntryPoint lets Hilt supply this screen's ViewModel.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.campusNote.text = getString(R.string.campus_note, BuildConfig.CAMPUS.replaceFirstChar { it.uppercase() })

        binding.loginButton.setOnClickListener { submit() }
        binding.firstNameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        binding.studentIdInput.doAfterTextChanged { viewModel.onInputChanged() }
        binding.firstNameInput.doAfterTextChanged { viewModel.onInputChanged() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    viewModel.loggedIn.collect { keypass ->
                        startActivity(DashboardActivity.newIntent(this@LoginActivity, keypass))
                        finish() // Back from the Dashboard closes the app instead of reopening Login
                    }
                }
            }
        }
    }

    private fun submit() {
        WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())
        viewModel.login(
            studentId = binding.studentIdInput.text?.toString().orEmpty(),
            firstName = binding.firstNameInput.text?.toString().orEmpty(),
        )
    }

    private fun render(state: LoginViewModel.UiState) {
        binding.studentIdLayout.error = state.studentIdError?.let { getString(it) }
        binding.firstNameLayout.error = state.firstNameError?.let { getString(it) }

        binding.errorText.isVisible = state.errorMessage != null
        state.errorMessage?.let { binding.errorText.setText(it) }

        binding.loginButton.isEnabled = !state.loading
        binding.loginButton.text = if (state.loading) "" else getString(R.string.log_in)
        binding.loginProgress.isVisible = state.loading
        binding.studentIdInput.isEnabled = !state.loading
        binding.firstNameInput.isEnabled = !state.loading
    }
}
