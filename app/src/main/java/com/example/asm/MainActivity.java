package com.example.asm;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private int i;
    public int j;
    private static int k;
    public static int q;
    int l;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        Test test = new Test();
        test.foo();

    }

    private void inc() {
        i++;
    }

    private static void ccc() {
        k++;
    }

    private int inc1() {
        i++;
        return i;
    }

    private static int ccc1() {
        k++;
        return k;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void doSomething(int a, short j, byte d, String string) {
        System.out.println("hello world");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class Test {
        void foo() {
            i++;
            System.out.println(i);
            i += 1;
            j++;
            System.out.println(j);
            k++;
            System.out.println(k);
            k = 10;
            q++;
            System.out.println(q);
            i = 1;
            inc();
            ccc();
            inc1();
            ccc1();
            doSomething(1, (short) 2, (byte) 3, "");
        }
    }
}