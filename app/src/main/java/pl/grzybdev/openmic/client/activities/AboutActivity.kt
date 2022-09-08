package pl.grzybdev.openmic.client.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.grzybdev.openmic.client.R


class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse(getString(R.string.AUTHOR_SEND_MAIL)) // only email apps should handle this
            startActivity(intent)
        }

        val author_webpage = findViewById<Button>(R.id.about_btn_author_website)
        author_webpage.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.AUTHOR_WEBSITE_URL))))
        }

        val openmic_webpage = findViewById<Button>(R.id.about_btn_openmic_website)
        openmic_webpage.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.OPENMIC_WEBSITE_URL))))
        }

        val openmic_github = findViewById<Button>(R.id.about_btn_source)
        openmic_github.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.OPENMIC_GITHUB_URL))))
        }

        val linkedin = findViewById<Button>(R.id.about_social_linkedin)
        linkedin.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.AUTHOR_LINKEDIN_URL))))
        }

        val reddit = findViewById<Button>(R.id.about_social_reddit)
        reddit.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.AUTHOR_REDDIT_URL))))
        }

        val twitter = findViewById<Button>(R.id.about_social_twitter)
        twitter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.AUTHOR_TWITTER_URL))))
        }
    }

}