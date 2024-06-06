package com.example.termp_new

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * InitActivity가 먼저 실행되고 권한을 요청함
 * 권한이 하나라도 거부되면 메세지를 출력하고 종료
 * 모든 권한이 승인되면 MainActivity로 넘어감
 */
class InitActivity : AppCompatActivity() {

    val REQUEST_CAMERA_PERMISSION = 200;
    val PERMISSIONS_REQUIRED = arrayOf(android.Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)

        // 모든 권한이 승인되었는지 확인함
        if (!hasPermissions(PERMISSIONS_REQUIRED)) {
            // 사용자에게 권한 요청
            requestPermissions(PERMISSIONS_REQUIRED, REQUEST_CAMERA_PERMISSION)
        }
        else {
            startActivity(Intent(this@InitActivity, MainActivity::class.java))
            finish()
        }

    }

    /**
     * permissions 배열에 있는 모든 권한이 허가되었는지 확인함
     * 모두다 허가되어 있다면 true, 하나라도 허가되어 있지 않다면 false를 리턴
     */
    private fun hasPermissions(permissions: Array<String>): Boolean {
        var result: Int

        // 배열에 있는 권한들 확인
        for (perms in permissions) {
            result = ContextCompat.checkSelfPermission(this, perms)
            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 권한 발견
                return false
            }
        }

        //모든 권한이 허가되었으므로 true 리턴
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한이 사용자에 의해 거부되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            startActivity(Intent(this@InitActivity, MainActivity::class.java))
            finish()
        }

    }
}