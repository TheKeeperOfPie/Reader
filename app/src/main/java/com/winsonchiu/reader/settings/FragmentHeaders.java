package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentHeaders.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentHeaders#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentHeaders extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private RecyclerView recyclerHeaders;
    private AdapterHeaders adapterHeaders;
    private Activity activity;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentHeaders.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentHeaders newInstance(String param1, String param2) {
        FragmentHeaders fragment = new FragmentHeaders();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentHeaders() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_headers, container, false);

        adapterHeaders = new AdapterHeaders(new AdapterHeaders.EventListener() {
            @Override
            public void onClickHeader(int position) {

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction().addToBackStack(null).hide(FragmentHeaders.this);

                switch (position) {
                    case 0:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentDisplay());
                        break;
                    case 1:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentBehavior());
                        break;
                    case 2:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentMail());
                        break;
                }

                fragmentTransaction.commit();
            }
        });

        recyclerHeaders = (RecyclerView) view.findViewById(R.id.recycler_headers);
        recyclerHeaders.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerHeaders.setHasFixedSize(true);
        recyclerHeaders.setAdapter(adapterHeaders);
        recyclerHeaders.setItemAnimator(null);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        }
//        catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
